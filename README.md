# Agentic Lender

Prototype lending platform split into four independently runnable subsystems:

| Subsystem | Purpose | Instructions |
|---|---|---|
| Web app | React/Vite staff prototype for authentication and a shared loan-application pipeline | [web-app/README.md](./web-app/README.md) |
| Mobile app | Android client prototype with Firebase Authentication, user profiles, and messaging | [mobile-app/README.md](./mobile-app/README.md) |
| AI engine | Python CLI committee for affordability, valuation, market context, and recommendations | [ai-engine/README.md](./ai-engine/README.md) |
| Ledger | Hardhat/Solidity prototype for immutable loan notes and the BER rate discount | [ledger/README.md](./ledger/README.md) |
| Firebase | Shared Firestore configuration, rules, and deployment instructions | [firebase/README.md](./firebase/README.md) |

## Important integration status

- Both clients currently target the Firebase project `benchodlender`.
- The mobile client saves profiles under `users/{uid}` and messages under
  `chats/{uid}/messages`. The web client stores flat application records in
  `applications`.
- The web application form only collects contact details and requested amount.
  The AI engine needs additional underwriting inputs and explicit
  `consent.aiAnalysis`; it will not assess a web-created record without that
  consent and data. There is no API or end-to-end hand-off between the AI
  engine, web/mobile clients, and ledger yet.
- The web pipeline is currently shared among all authenticated accounts. The
  accompanying Firestore rules preserve this demo behavior; do not use them
  for real borrower data without adding broker/underwriter roles and narrower
  access controls.
- The AI valuation model and market context are prototype inputs. Review the
  AI engine README before relying on any output.

Each subsystem has its own setup, run, and test instructions in its README.