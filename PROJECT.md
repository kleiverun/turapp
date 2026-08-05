# Turapp

## Problem
Hikers have no easy way to find the routes they've walked before.
Routes disappear as soon as the hike is over, and there's no personal
archive of where you've actually been.

## Target audience
People who enjoy hiking, including off marked trails, and who want
to preserve and revisit their own routes.

## MVP – build this first
The core features that must be in place for the app to provide real value.
The order follows a natural build sequence — each item builds on the previous one.

1. User can register and log in ✅
2. A map is displayed in the app ✅
3. The user's own position is shown on the map in real time (via the Android device's GPS)
4. User can start a trip recording, which tracks the route as they go, and stop it again
5. User can view their past trips, both as a list and as routes/markers on the map ✅ (web)

## Out of scope (for now)
- Sharing trips with others
- Viewing other users' saved trips on the map
- Hiker groups / social features

## Technology choices
- **Spring Boot** (backend): good stability and uptime, and builds on Java, which I know best
- **React + TypeScript** (web frontend): route planning and trip overview in the browser; simpler to iterate on than the Android app
- **Android Studio** (mobile frontend): live GPS tracking during hikes — the main field tool
- **MySQL** (database): the relational database I know best, and the data model here has no advanced relationships that would require anything more

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| User gains access to another user's trip data | Medium | Check ownership on every request — a user can only retrieve their own resources, even with a valid login |
| SQL injection | Low | Use JPA repositories and parameterized queries (`@Query` with parameters) — never build SQL by concatenating strings |
| Unauthorized access to protected endpoints | Medium | JWT required on all endpoints except registration, login, and public map overlays; validate all input server-side |
| Scope creep (project grows uncontrollably) | High | Finish the MVP features first; new ideas go into "Out of scope" or the backlog, not built now |
| Features not finished / falling behind | Medium | Break each feature into pieces that can be completed in one session, so something always gets done |
| GPS tracking drains the battery on longer hikes | Medium | Test power consumption early; consider adjusting how often position is sampled |
| Trip data lost if the app crashes mid-hike | Medium | Save tracking points continuously during the trip, not just when the user stops it |
