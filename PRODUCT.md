# Product

<!-- impeccable:product-schema 1 -->

## Platform

web

## Users

Learners preparing for structured study goals who want one place to study, practice, track progress, and stay motivated.

## Product Purpose

LearningProgram helps learners build a consistent study habit by combining courses, vocabulary and knowledge practice, progress tracking, social learning, and lightweight rewards.

## Positioning

A unified learning loop connects structured courses and practice with accountability features such as study groups, check-ins, points, rankings, and discussion.

## Operating Context

Learners use the web application for account-based study workflows: they browse courses and chapters, practice vocabulary and knowledge tests, record progress, participate in discussions or study groups, check in, and review notifications and rankings. Administrators manage users, learning content, question banks, study sets, resources, and directed notifications.

## Capabilities and Constraints

- The product includes registration, username or email login, remember-me sessions, JWT authentication, password recovery, profiles, courses, chapters, CET-4/CET-6 vocabulary, vocabulary and knowledge tests, dictionary lookup, daily words, learning resources, discussions, comments, study groups, group messages and tasks, check-ins, points, rankings, notifications, and an administration area.
- Preserve the existing Vue 3, TypeScript, Vite, Vue Router, Vue i18n, Element Plus, Axios, and SSR-capable web stack.
- Chinese is the first supported application language.
- Learner and administrator roles are part of the product model. Administrator access is protected by the backend security layer.
- Frontend requests use same-origin relative `/api` paths, with development proxying to the backend.
- Authentication state is stored in the existing `authorize` local/session storage flow and enforced through JWT-aware backend and frontend behavior.
- Future work must preserve existing product terminology and factual behavior. Pricing, testimonials, benchmarks, certifications, and other unsupported claims must not be invented.

## Brand Commitments

The existing product name is LearningProgram. The product is Chinese-first and currently presents itself as a practical learning platform rather than a credentialing or commercial course marketplace.

## Evidence on Hand

- Product scope and technical stack: [README.md](./README.md)
- Frontend workflows and runtime constraints: [learning-program-frontend/README.md](./learning-program-frontend/README.md)
- Backend and frontend API behavior: [docs/API.md](./docs/API.md)
- Data model and domain relationships: [docs/DATABASE.md](./docs/DATABASE.md)
- Deployment and runtime setup: [docs/DEPLOYMENT.md](./docs/DEPLOYMENT.md)
- Existing application routes and screens: [learning-program-frontend/src/router](./learning-program-frontend/src/router) and [learning-program-frontend/src/views](./learning-program-frontend/src/views)
- No testimonials, customer studies, performance benchmarks, pricing, or certifications are established in the repository.

## Product Principles

- Make consistent study easier than fragmented study.
- Connect learning activity to visible progress and meaningful feedback.
- Use social accountability as support, not as a substitute for learning.
- Keep learner and administrator workflows clear, role-appropriate, and trustworthy.
- Prefer factual, useful communication over unsupported promises.

## Accessibility & Inclusion

No product-specific accessibility standard has been established beyond the requirements of a web application. Future work should preserve keyboard-accessible controls, readable Chinese-first typography, sufficient contrast, and clear states for loading, empty, error, and authenticated flows.
