# Smart-Attendance-AI

Real-Time Attendance Conflict Resolution System built with **Spring Boot 3**, **H2/MySQL**, and a glassmorphic **React + Vite** dashboard.

## Overview
An enterprise-grade, deterministic attendance reconciliation engine designed to ingest out-of-order, duplicate, and conflicting multi-modal attendance events (Cameras, Mobile Apps, Manual Check-ins) and resolve them into accurate attendance sessions.

## Core Features
1. **Identity Resolution Engine**:
   - Evaluates face confidence thresholds ($\ge 0.8$).
   - Validates matching face templates and 30-day enrollment windows.
   - Flags low confidence or missing/expired identities as `PENDING`.
2. **State Reconstruction & Temporal Replay**:
   - Deterministic event resolution using source precedence (`camera` > `app` > `manual`).
   - Automatically handles out-of-order checkouts before check-ins and late-arriving events.
   - Computes deterministic final session states (`valid`, `conflict_resolved`, `pending`, `invalid`).
3. **Glassmorphic React Dashboard**:
   - Real-time monitoring of session statuses and timeline logs.
   - Fixture-based edge-case simulator for zero-setup verification.

## Setup & Running

### Backend (Spring Boot 3)
```bash
mvn clean spring-boot:run
```
Runs on `http://localhost:8080`.

### Frontend (React + Vite)
```bash
cd frontend
npm install
npm run dev
```
Runs on `http://localhost:5173`.

## Edge-Case Simulation
Use the built-in **Edge Case Simulator** tab in the dashboard to execute automated replay tests for 5 complex scenarios:
1. Idempotent duplicate events
2. Out-of-order checkouts
3. Source conflicts (Camera vs. App)
4. Low face confidence (< 0.8) & pending identity drift
5. Late event reconciliation
