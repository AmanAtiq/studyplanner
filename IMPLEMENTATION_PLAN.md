# Study Assistant - New Features Implementation Plan

## 1. Subject Selection on Note Upload
- Update UploadViewModel to include subject selection UI
- Store selected subjectId with note
- Allow optional subject (empty string default)
- Show subject info on note cards

## 2. Performance Analytics
**Data Model:**
- QuizPerformanceStats: Track scores per subject/quiz
- AnalyticsData: Aggregate user performance metrics

**Features:**
- Calculate average score per subject
- Track score trends over time
- Identify best/worst performing subjects
- Performance comparison charts

**Implementation:**
- Create AnalyticsRepository interface
- Add methods to compute performance metrics from quiz history
- Build PerformanceAnalyticsScreen UI component

## 3. Leaderboards
**Data Model:**
- LeaderboardEntry: userId, username, averageScore, totalQuizzes, rank
- LeaderboardType enum: GLOBAL, SUBJECT_SPECIFIC

**Features:**
- Global leaderboard (all users)
- Subject-specific leaderboards
- Rank based on average quiz score
- Show user position and nearby competitors

**Implementation:**
- Create LeaderboardRepository
- Add aggregation queries for ranking
- Build LeaderboardScreen with tabs for different rankings

## 4. Study Groups
**Data Models:**
- StudyGroup: id, createdBy, groupName, description, createdAt, inviteLink, isActive
- StudyGroupMember: userId, groupId, joinedAt, role (ADMIN, MEMBER)
- GroupMessage: id, senderId, groupId, message, timestamp, senderName

**Features:**
- Create group with unique invite link
- Add members via link or direct invitation
- Real-time group chat/discussion
- Group member list with roles
- Leave/delete group functionality

**Implementation:**
- Create StudyGroup entities and DAOs
- Generate shareable invite links (UUID-based)
- Build real-time messaging with Firebase
- Create GroupChatScreen, GroupListScreen, CreateGroupScreen
- Add group member management UI

## Database Changes
- Add StudyGroup, StudyGroupMember, GroupMessage entities
- Create StudyGroupDao
- Update AppDatabase version to 3

## UI Screens to Add
1. PerformanceAnalyticsScreen - Charts and metrics
2. LeaderboardScreen - Rankings view
3. StudyGroupListScreen - My groups
4. CreateGroupScreen - Create new group
5. GroupChatScreen - Real-time chat
6. GroupMembersScreen - Member management

## Navigation Updates
- Add routes for new screens in NavGraph
- Update BottomBar with new section (Analytics/Groups)

## Firebase Integration
- Add Firestore collections for groups and messages
- Set up real-time listeners for chat
- Configure security rules for group access
