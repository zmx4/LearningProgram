package com.tick.service.impl;

import com.pig4cloud.plugin.oss.service.OssTemplate;
import com.tick.entity.dto.Account;
import com.tick.mapper.AccountMapper;
import com.tick.storage.StoredFile;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockMultipartFile;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 头像服务：仅对象存储启用时可用；上传走类型 / 大小 / 魔数校验，
 * 对象键落在 avatars/ 前缀下，替换与移除时清理旧文件。
 */
class AvatarServiceImplTest {

    private static final String BUCKET = "learning-resources";
    private static final byte[] PNG_BYTES = {
            (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 13, 'I', 'H', 'D', 'R'};

    private final OssTemplate ossTemplate = mock(OssTemplate.class);
    @SuppressWarnings("unchecked")
    private final ObjectProvider<OssTemplate> ossProvider = mock(ObjectProvider.class);
    private final AccountMapper accountMapper = mock(AccountMapper.class);

    private AvatarServiceImpl service(boolean ossAvailable) {
        when(ossProvider.getIfAvailable()).thenReturn(ossAvailable ? ossTemplate : null);
        return new AvatarServiceImpl(ossProvider, accountMapper, BUCKET);
    }

    private Account account(String avatar) {
        Account account = new Account(1, "tick", "encoded", "tick@example.com", "user", new Date());
        account.setAvatar(avatar);
        when(accountMapper.selectById(1)).thenReturn(account);
        return account;
    }

    @Test
    void uploadRejectedWhenObjectStorageDisabled() {
        AvatarServiceImpl service = service(false);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.upload(1, new MockMultipartFile("file", "a.png", "image/png", PNG_BYTES)));
        assertTrue(exception.getMessage().contains("对象存储"));
        assertFalse(service.enabled());
        verifyNoInteractions(accountMapper);
    }

    @Test
    void uploadRejectsNonImageType() {
        AvatarServiceImpl service = service(true);
        account(null);

        assertThrows(IllegalArgumentException.class, () -> service.upload(1,
                new MockMultipartFile("file", "a.html", "text/html", "<html></html>".getBytes())));
        verifyNoInteractions(ossTemplate);
    }

    @Test
    void uploadRejectsOversizedFile() {
        AvatarServiceImpl service = service(true);
        account(null);
        byte[] tooLarge = new byte[2 * 1024 * 1024 + 1];

        assertThrows(IllegalArgumentException.class, () -> service.upload(1,
                new MockMultipartFile("file", "a.png", "image/png", tooLarge)));
        verifyNoInteractions(ossTemplate);
    }

    @Test
    void uploadRejectsContentNotMatchingDeclaredType() {
        AvatarServiceImpl service = service(true);
        account(null);

        // 声明是 PNG，内容却是 HTML：魔数校验必须拦下
        assertThrows(IllegalArgumentException.class, () -> service.upload(1,
                new MockMultipartFile("file", "a.png", "image/png", "<html></html>".getBytes())));
        verifyNoInteractions(ossTemplate);
    }

    @Test
    void uploadStoresUnderAvatarsPrefixAndCleansPrevious() {
        AvatarServiceImpl service = service(true);
        account("old.png");
        assertTrue(service.enabled());

        Account updated = service.upload(1, new MockMultipartFile("file", "a.png", "image/png", PNG_BYTES));

        String storedName = updated.getAvatar();
        assertNotEquals("old.png", storedName);
        assertTrue(storedName.endsWith(".png"));
        verify(ossTemplate).putObject(eq(BUCKET), startsWith("avatars/"),
                any(InputStream.class), anyLong(), eq("image/png"));
        verify(ossTemplate).removeObject(BUCKET, "avatars/old.png");
        verify(accountMapper).update(isNull(), any());
    }

    @Test
    void removeClearsAvatarAndDeletesObject() {
        AvatarServiceImpl service = service(true);
        Account account = account("old.png");

        Account updated = service.remove(1);

        assertNull(updated.getAvatar());
        assertSame(account, updated);
        verify(ossTemplate).removeObject(BUCKET, "avatars/old.png");
        verify(accountMapper).update(isNull(), any());
    }

    @Test
    void removeWithoutObjectStorageStillClearsRecord() {
        AvatarServiceImpl service = service(false);
        account("old.png");

        Account updated = service.remove(1);

        assertNull(updated.getAvatar());
        verify(accountMapper).update(isNull(), any());
        verifyNoInteractions(ossTemplate);
    }

    @Test
    void removeWithoutAvatarSkipsWrites() {
        AvatarServiceImpl service = service(true);
        account(null);

        service.remove(1);

        verify(accountMapper, never()).update(isNull(), any());
        verifyNoInteractions(ossTemplate);
    }

    @Test
    void openReturnsNullWhenDisabledOrUnset() {
        AvatarServiceImpl disabled = service(false);
        assertNull(disabled.open(account("old.png")));

        AvatarServiceImpl enabled = service(true);
        assertNull(enabled.open(account(null)));
        assertNull(enabled.open(null));
    }

    @Test
    void openStreamsAvatarFromAvatarsPrefix() {
        AvatarServiceImpl service = service(true);
        InputStream stream = new ByteArrayInputStream(PNG_BYTES);
        when(ossTemplate.getObject(BUCKET, "avatars/old.png")).thenReturn(stream);

        StoredFile file = service.open(account("old.png"));

        assertSame(stream, file.stream());
    }

    @Test
    void openReturnsNullWhenObjectMissing() {
        AvatarServiceImpl service = service(true);
        when(ossTemplate.getObject(BUCKET, "avatars/old.png"))
                .thenThrow(S3Exception.builder().statusCode(404).build());

        assertNull(service.open(account("old.png")));
    }

    @Test
    void missingAccountReturnsNull() {
        AvatarServiceImpl service = service(true);
        when(accountMapper.selectById(99)).thenReturn(null);

        assertNull(service.upload(99, new MockMultipartFile("file", "a.png", "image/png", PNG_BYTES)));
        assertNull(service.remove(99));
    }
}