# Graph Report - Simha  (2026-11-23)

## Corpus Check
- cluster-only mode — file stats not available

## Summary
- 849 nodes · 2109 edges · 29 communities (14 shown, 15 thin omitted)
- Extraction: 92% EXTRACTED · 8% INFERRED · 0% AMBIGUOUS · INFERRED: 162 edges (avg confidence: 0.83)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `a5ca1059`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- MainViewModel
- FakeHost
- Repo
- ToolHost
- Actions.kt
- Voice.kt
- PlansTest
- .decide
- with
- Work.kt
- LlmClient.kt
- ActivityService.kt
- Prefs.kt
- MainActivity.kt
- TalkViewModel.kt
- Theme.kt
- Kind
- ProvideLayout
- TalkMode
- CallResult
- Prompt

## God Nodes (most connected - your core abstractions)
1. `MainViewModel` - 48 edges
2. `Repo` - 45 edges
3. `jo()` - 36 edges
4. `FakeHost` - 29 edges
5. `SettingsScreen()` - 26 edges
6. `ToolHost` - 24 edges
7. `TalkViewModel` - 24 edges
8. `BigButton()` - 24 edges
9. `Snap` - 23 edges
10. `Body()` - 23 edges

## Surprising Connections (you probably didn't know these)
- `SimhaRoot()` --calls--> `ProvideLayout()`  [INFERRED]
  app/src/main/java/com/simha/app/ui/App.kt → app/src/main/java/com/simha/app/ui/Components.kt
- `MainActivity` --references--> `MainViewModel`  [EXTRACTED]
  app/src/main/java/com/simha/app/MainActivity.kt → app/src/main/java/com/simha/app/MainViewModel.kt
- `MainViewModel` --references--> `ContactEntity`  [EXTRACTED]
  app/src/main/java/com/simha/app/MainViewModel.kt → app/src/main/java/com/simha/app/data/Db.kt
- `MainViewModel` --references--> `ProfileEntity`  [EXTRACTED]
  app/src/main/java/com/simha/app/MainViewModel.kt → app/src/main/java/com/simha/app/data/Db.kt
- `ServiceLocator` --calls--> `LlmClient`  [EXTRACTED]
  app/src/main/java/com/simha/app/SimhaApp.kt → app/src/main/java/com/simha/app/llm/LlmClient.kt

## Import Cycles
- None detected.

## Communities (29 total, 15 thin omitted)

### Community 0 - "MainViewModel"
Cohesion: 0.05
Nodes (45): Snap, Step, MainViewModel, AppNav(), SimhaRoot(), AuditScreen(), CaregiverHomeScreen(), CaregiverSetupScreen() (+37 more)

### Community 1 - "FakeHost"
Cohesion: 0.06
Nodes (19): Agent, ChatApi, LlmClient, LlmConfig, History, LlmException, LlmParse, Msg (+11 more)

### Community 2 - "Repo"
Cohesion: 0.07
Nodes (18): ContactEntity, OutboxEntity, ProfileEntity, SimhaDao, SimhaDb, AuditRow, CallRow, CaregiverRow (+10 more)

### Community 3 - "ToolHost"
Cohesion: 0.05
Nodes (7): SimhaTools, ToolHost, TalkScreen(), ChatLine, TalkViewModel, Host, UiMsg

### Community 4 - "Actions.kt"
Cohesion: 0.06
Nodes (14): Actions, forLang(), Perm, Battery, CallPhone, Camera, FullScreen, Location (+6 more)

### Community 5 - "Voice.kt"
Cohesion: 0.06
Nodes (14): Langs, Recorder, Voice, RecognitionListener, UtteranceProgressListener, VoiceFail, VoiceIssue, LangMissing (+6 more)

### Community 6 - "PlansTest"
Cohesion: 0.07
Nodes (25): Ago, Days, Hours, Minutes, Never, Now, Answer, Fine (+17 more)

### Community 8 - ".decide"
Cohesion: 0.12
Nodes (11): K, Approvals, Decision, AlertFamily, EscalateCallFamily, None, PromptCheckin, Rules (+3 more)

### Community 9 - "with"
Cohesion: 0.06
Nodes (30): runs-on, steps, concurrency, cancel-in-progress, group, KEYSTORE_BASE64, jobs, build (+22 more)

### Community 10 - "Work.kt"
Cohesion: 0.10
Nodes (4): ActivityWorker, ReminderWorker, SyncWorker, Work

### Community 19 - "Kind"
Cohesion: 0.40
Nodes (5): Kind, Emergency, Primary, Secondary, Success

### Community 20 - "ProvideLayout"
Cohesion: 0.50
Nodes (3): Layout, ProvideLayout(), rememberLayout()

### Community 21 - "TalkMode"
Cohesion: 0.40
Nodes (5): TalkMode, Idle, Listening, Speaking, Thinking

### Community 22 - "CallResult"
Cohesion: 0.50
Nodes (4): CallResult, Called, Dialer, Failed

## Knowledge Gaps
- **44 isolated node(s):** `OpenEmergency`, `Never`, `Now`, `AlertFamily`, `EscalateCallFamily` (+39 more)
  These have ≤1 connection - possible missing edges. (Counts symbols only; 234 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **15 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `Repo` connect `Repo` to `MainViewModel`, `Voice.kt`, `.decide`, `Repo.kt`, `Prefs.kt`, `TalkViewModel.kt`?**
  _High betweenness centrality (0.109) - this node is a cross-community bridge._
- **Are the 16 inferred relationships involving `jo()` (e.g. with `.addContact()` and `.audit()`) actually correct?**
  _`jo()` has 16 INFERRED edges - model-reasoned connections that need verification._
- **What connects `OpenEmergency`, `Never`, `Now` to the rest of the system?**
  _44 weakly-connected nodes found - possible missing edges._
- **Should `MainViewModel` be split into smaller, more focused modules?**
  _Cohesion score 0.050387596899224806 - nodes in this community are weakly interconnected._
- **Why does `jo()` connect `Repo` to `MainViewModel`, `ToolHost`, `Actions.kt`, `.decide`, `Work.kt`, `TalkViewModel.kt`?**
  _High betweenness centrality (0.080) - this node is a cross-community bridge._
- **Are the 9 inferred relationships involving `SettingsScreen()` (e.g. with `AppNav()` and `BigButton()`) actually correct?**
  _`SettingsScreen()` has 9 INFERRED edges - model-reasoned connections that need verification._
- **Should `FakeHost` be split into smaller, more focused modules?**
  _Cohesion score 0.05898876404494382 - nodes in this community are weakly interconnected._