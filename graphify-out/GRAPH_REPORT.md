# Graph Report - Simha  (2026-11-23)

## Corpus Check
- 47 files · ~29,518 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 40 file(s) not represented in the graph (top: .xml 36, (none) 1, .ttf 1)
- WARNING: 40 of 95 files seen (42%) have no supported extractor and are missing from this graph entirely. It may look complete while representing only part of the codebase.

## Summary
- 1011 nodes · 2558 edges · 50 communities (19 shown, 31 thin omitted)
- Extraction: 94% EXTRACTED · 6% INFERRED · 0% AMBIGUOUS · INFERRED: 162 edges (avg confidence: 0.79)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `6414a089`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- MainViewModel
- FakeHost
- Repo
- TalkViewModel.kt
- Actions.kt
- Voice.kt
- PlansTest
- Components.kt
- .decide
- with
- Work.kt
- ActivityService.kt
- MainActivity.kt
- Theme.kt
- Settings.kt
- AiSettings.kt
- SarvamConfig
- Onboarding.kt
- Screens.kt
- SarvamHealthTest
- .execute
- ToolHost
- HealthHost
- HealthMemory
- EpState
- Wav

## God Nodes (most connected - your core abstractions)
1. `Repo` - 48 edges
2. `MainViewModel` - 42 edges
3. `jo()` - 39 edges
4. `FakeHost` - 30 edges
5. `SettingsScreen()` - 28 edges
6. `ToolHost` - 27 edges
7. `Voice` - 27 edges
8. `Snap` - 24 edges
9. `TalkViewModel` - 24 edges
10. `LlmToolsTest` - 22 edges

## Surprising Connections (you probably didn't know these)
- `SimhaRoot()` --calls--> `ProvideLayout()`  [INFERRED]
  app/src/main/java/com/simha/app/ui/App.kt → app/src/main/java/com/simha/app/ui/Components.kt
- `AppNav()` --calls--> `animMs()`  [INFERRED]
  app/src/main/java/com/simha/app/ui/App.kt → app/src/main/java/com/simha/app/ui/Components.kt
- `AppNav()` --calls--> `HomeScreen()`  [INFERRED]
  app/src/main/java/com/simha/app/ui/App.kt → app/src/main/java/com/simha/app/ui/Home.kt
- `AppNav()` --calls--> `ContactsScreen()`  [INFERRED]
  app/src/main/java/com/simha/app/ui/App.kt → app/src/main/java/com/simha/app/ui/Screens.kt
- `AppNav()` --calls--> `EmergencyScreen()`  [INFERRED]
  app/src/main/java/com/simha/app/ui/App.kt → app/src/main/java/com/simha/app/ui/Screens.kt

## Import Cycles
- None detected.

## Communities (50 total, 31 thin omitted)

### Community 0 - "MainViewModel"
Cohesion: 0.12
Nodes (15): Snap, MainViewModel, AppNav(), SimhaRoot(), AuditScreen(), CaregiverHomeScreen(), CaregiverSetupScreen(), eventLabel() (+7 more)

### Community 1 - "FakeHost"
Cohesion: 0.06
Nodes (18): Agent, ChatApi, LlmClient, LlmConfig, History, LlmException, LlmParse, Msg (+10 more)

### Community 2 - "Repo"
Cohesion: 0.08
Nodes (16): ContactEntity, OutboxEntity, ProfileEntity, SimhaDao, SimhaDb, AuditRow, CallRow, CaregiverRow (+8 more)

### Community 3 - "TalkViewModel.kt"
Cohesion: 0.08
Nodes (14): TalkScreen(), ChatLine, Prompt, TalkEvent, OpenEmergency, TalkMode, Idle, Listening (+6 more)

### Community 4 - "Actions.kt"
Cohesion: 0.05
Nodes (14): Actions, Perm, Battery, CallPhone, Camera, FullScreen, Location, Mic (+6 more)

### Community 5 - "Voice.kt"
Cohesion: 0.05
Nodes (11): Langs, Recorder, SpeakItem, Voice, RecognitionListener, UtteranceProgressListener, VoiceFail, VoiceKind (+3 more)

### Community 6 - "PlansTest"
Cohesion: 0.07
Nodes (25): Ago, Days, Hours, Minutes, Never, Now, Answer, Fine (+17 more)

### Community 7 - "Components.kt"
Cohesion: 0.06
Nodes (6): Layout, ProvideLayout(), rememberLayout(), ActionTile(), HeroCard(), HomeScreen()

### Community 8 - ".decide"
Cohesion: 0.12
Nodes (12): forLang(), K, Approvals, Decision, AlertFamily, EscalateCallFamily, None, PromptCheckin (+4 more)

### Community 9 - "with"
Cohesion: 0.06
Nodes (30): runs-on, steps, concurrency, cancel-in-progress, group, KEYSTORE_BASE64, jobs, build (+22 more)

### Community 10 - "Work.kt"
Cohesion: 0.06
Nodes (8): OBK, Prefs, ActivityWorker, ReminderWorker, SyncWorker, Work, ServiceLocator, SimhaApp

### Community 12 - "ActivityService.kt"
Cohesion: 0.06
Nodes (11): CallResult, Called, Dialer, Failed, LlmStore, ActivityService, Escalation, AgentCall (+3 more)

### Community 18 - "Theme.kt"
Cohesion: 0.11
Nodes (3): SimhaColors, scheme(), SimhaTheme()

### Community 19 - "Settings.kt"
Cohesion: 0.08
Nodes (7): Kind, Emergency, Primary, Secondary, Success, hoursText(), minutesText()

### Community 20 - "AiSettings.kt"
Cohesion: 0.18
Nodes (28): animMs(), BigButton(), Body(), ButtonCore(), Caption(), Card(), Chip(), ChipGroup() (+20 more)

### Community 21 - "SarvamConfig"
Cohesion: 0.21
Nodes (4): SarvamConfig, Sarvam, SarvamException, SarvamClient

### Community 22 - "Onboarding.kt"
Cohesion: 0.10
Nodes (7): openMaps(), PermText, permText(), VoiceIssue, LangMissing, None, Unavailable

### Community 23 - "Screens.kt"
Cohesion: 0.17
Nodes (5): Step, openMaps(), PickPhone, rememberPicker(), stepText()

### Community 43 - "EpState"
Cohesion: 0.50
Nodes (4): EpState, Done, Timeout, Waiting

## Knowledge Gaps
- **47 isolated node(s):** `Primary`, `Emergency`, `Secondary`, `Success`, `Idle` (+42 more)
  These have ≤1 connection - possible missing edges. (Counts symbols only; 227 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **31 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `Repo` connect `Repo` to `TalkViewModel.kt`, `Voice.kt`, `Components.kt`, `.decide`, `Work.kt`, `Repo.kt`, `Settings.kt`, `Onboarding.kt`, `Screens.kt`?**
  _High betweenness centrality (0.103) - this node is a cross-community bridge._
- **Are the 16 inferred relationships involving `jo()` (e.g. with `.addContact()` and `.audit()`) actually correct?**
  _`jo()` has 16 INFERRED edges - model-reasoned connections that need verification._
- **What connects `Primary`, `Emergency`, `Secondary` to the rest of the system?**
  _47 weakly-connected nodes found - possible missing edges._
- **Should `MainViewModel` be split into smaller, more focused modules?**
  _Cohesion score 0.12310606060606061 - nodes in this community are weakly interconnected._
- **Why does `ToolHost` connect `ToolHost` to `HealthHost`, `FakeHost`, `.audit`, `.dialEmergency`, `.logSymptom`, `.markCheckinOk`, `.openEmergencyScreen`, `.recordingApproved`, `.rememberHealthDetail`, `.setReminder`, `.updateProfileField`, `TalkViewModel.kt`, `LlmParse.kt`, `.execute`, `.getProfile`?**
  _High betweenness centrality (0.078) - this node is a cross-community bridge._
- **Are the 11 inferred relationships involving `SettingsScreen()` (e.g. with `AppNav()` and `Body()`) actually correct?**
  _`SettingsScreen()` has 11 INFERRED edges - model-reasoned connections that need verification._
- **Should `FakeHost` be split into smaller, more focused modules?**
  _Cohesion score 0.057195149851292613 - nodes in this community are weakly interconnected._