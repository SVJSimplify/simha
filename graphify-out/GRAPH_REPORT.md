# Graph Report - Simha  (2026-11-25)

## Corpus Check
- 102 files · ~67,032 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 84 file(s) not represented in the graph (top: .xml 80, (none) 1, .ttf 1)
- WARNING: 84 of 195 files seen (43%) have no supported extractor and are missing from this graph entirely. It may look complete while representing only part of the codebase.

## Summary
- 2076 nodes · 5955 edges · 147 communities (53 shown, 94 thin omitted)
- Extraction: 94% EXTRACTED · 6% INFERRED · 0% AMBIGUOUS · INFERRED: 330 edges (avg confidence: 0.82)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `c9c16c40`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- Body
- ContactInfo
- Db.kt
- TalkViewModel
- Actions
- MedicalProfile
- PlansTest
- jo
- .decide
- with
- UpdateWorker.kt
- Wake.kt
- GuardianRepo
- Home.kt
- Weather.kt
- Query
- MainActivity.kt
- Onboarding.kt
- WakeWord.kt
- SarvamConfig
- Sig
- Models.kt
- .stream
- Theme.kt
- .execute
- WakeMathTest
- Situation
- MainViewModel
- ActivityService.kt
- .learn
- Perm
- Voice.kt
- .wrapped
- Updater.kt
- FakeHost
- Reviewer.kt
- Repo.kt
- MsgRecorder
- .aux
- .finish
- TalkViewModel.kt
- Voice.kt
- LlmStore
- UpdateInstallReceiver.kt
- .intake
- LlmToolsTest.kt
- .run
- SafeArea.kt
- Open
- WakeReq
- .envelope
- SimhaApp.kt
- UpdateState.kt
- GNode
- CareEngine
- .uid
- .addMemory
- Components.kt
- WakeReason
- DoseLog
- ConversationPanel.kt
- CaregiverStats.kt
- Oem
- Notifier.kt
- TaskType
- .pollFamily
- GraphDao
- .resolve
- parseTime
- .retrieve
- RecPhase
- SarvamHealthTest
- Work
- Wake.kt
- NodeType
- CareTest
- Context
- LocaleCtx.kt
- Kind
- SlotKind
- FillerGate
- Context
- SimhaColors
- Wav

## God Nodes (most connected - your core abstractions)
1. `MainViewModel` - 74 edges
2. `jo()` - 64 edges
3. `FakeHost` - 51 edges
4. `Repo` - 50 edges
5. `Wake` - 45 edges
6. `ToolHost` - 38 edges
7. `Body()` - 38 edges
8. `LlmToolsTest` - 37 edges
9. `TalkViewModel` - 35 edges
10. `GuardianRepo` - 32 edges

## Surprising Connections (you probably didn't know these)
- `SimhaRoot()` --calls--> `ProvideLayout()`  [INFERRED]
  app/src/main/java/com/simha/app/ui/App.kt → app/src/main/java/com/simha/app/ui/Components.kt
- `AppNav()` --calls--> `CaregiverHomeScreen()`  [INFERRED]
  app/src/main/java/com/simha/app/ui/App.kt → app/src/main/java/com/simha/app/ui/Caregiver.kt
- `AppNav()` --calls--> `animMs()`  [INFERRED]
  app/src/main/java/com/simha/app/ui/App.kt → app/src/main/java/com/simha/app/ui/Components.kt
- `AppNav()` --calls--> `HomeScreen()`  [INFERRED]
  app/src/main/java/com/simha/app/ui/App.kt → app/src/main/java/com/simha/app/ui/Home.kt
- `AppNav()` --calls--> `OnboardingScreen()`  [INFERRED]
  app/src/main/java/com/simha/app/ui/App.kt → app/src/main/java/com/simha/app/ui/Onboarding.kt

## Import Cycles
- None detected.

## Communities (147 total, 94 thin omitted)

### Community 0 - "Body"
Cohesion: 0.14
Nodes (43): AppNav(), AuditScreen(), CaregiverSetupScreen(), AnimatedEntrance(), BigButton(), Body(), ButtonCore(), Caption() (+35 more)

### Community 2 - "Db.kt"
Cohesion: 0.17
Nodes (8): ContactEntity, DailySignalEntity, FactEntity, MedicineEntity, OutboxEntity, ProfileEntity, SimhaDb, WeeklySignalEntity

### Community 3 - "TalkViewModel"
Cohesion: 0.15
Nodes (3): ChatLine, TalkViewModel, UiMsg

### Community 5 - "MedicalProfile"
Cohesion: 0.17
Nodes (7): ContactLine, Guardian, GuardianContext, Known, MedLine, MedicalProfile, OpeningFillerTest

### Community 6 - "PlansTest"
Cohesion: 0.07
Nodes (25): Ago, Days, Hours, Minutes, Never, Now, Answer, Fine (+17 more)

### Community 7 - "jo"
Cohesion: 0.21
Nodes (3): jo(), Prefs, Repo

### Community 8 - ".decide"
Cohesion: 0.17
Nodes (9): Approvals, Decision, AlertFamily, EscalateCallFamily, None, PromptCheckin, Rules, RulesEngine (+1 more)

### Community 9 - "with"
Cohesion: 0.06
Nodes (33): runs-on, steps, concurrency, cancel-in-progress, group, env, KEYSTORE_BASE64, VERSION_CODE (+25 more)

### Community 15 - "Home.kt"
Cohesion: 0.05
Nodes (11): TodayItem, TodayState, BottomBar(), tabSwitch(), agoText(), Hairline(), HomeScreen(), ListRow() (+3 more)

### Community 19 - "Onboarding.kt"
Cohesion: 0.07
Nodes (12): OBK, ChoiceCard(), fadeUp(), OnboardingScreen(), openMaps(), Progress(), StepName(), StepWho() (+4 more)

### Community 20 - "WakeWord.kt"
Cohesion: 0.06
Nodes (6): K, UiKeys, WakeWordEngine, RecognitionListener, WakeWordMatcher, WakeWordPrefs

### Community 21 - "SarvamConfig"
Cohesion: 0.21
Nodes (4): SarvamConfig, Sarvam, SarvamException, SarvamClient

### Community 22 - "Sig"
Cohesion: 0.20
Nodes (4): Rollup, Sig, Signal, SignalTest

### Community 23 - "Models.kt"
Cohesion: 0.26
Nodes (11): AuditRow, CallRow, CaregiverRow, DailySignalRow, EventRow, FamilyMessage, RecordingRow, UserRow (+3 more)

### Community 25 - ".stream"
Cohesion: 0.22
Nodes (7): LlmClient, LlmConfig, LlmException, LlmParse, StreamAcc, CallAcc, ToolCall

### Community 26 - "Theme.kt"
Cohesion: 0.11
Nodes (3): nunito(), scheme(), SimhaTheme()

### Community 29 - ".execute"
Cohesion: 0.04
Nodes (4): SimhaTools, ToolHost, Host, HealthHost

### Community 30 - "WakeMathTest"
Cohesion: 0.25
Nodes (3): Slot, WakeMath, WakeMathTest

### Community 31 - "Situation"
Cohesion: 0.06
Nodes (24): DiaryMerge, Scam, Sig, ScamReport, FirstAid, Guide, Situation, Allergy (+16 more)

### Community 32 - "MainViewModel"
Cohesion: 0.12
Nodes (13): Snap, MainViewModel, SectionTitle(), SwitchRow(), medLine(), showTime(), AboutPage(), CheckinsPage() (+5 more)

### Community 33 - "ActivityService.kt"
Cohesion: 0.07
Nodes (10): ActivityService, FallDetector, FallMachine, Phase, Free, Idle, Impact, AlarmReceiver (+2 more)

### Community 34 - ".learn"
Cohesion: 0.20
Nodes (3): NightState, Routine, RoutineTest

### Community 35 - "Perm"
Cohesion: 0.16
Nodes (13): Perm, Battery, CallPhone, Camera, FullScreen, Mic, Notifications, Overlay (+5 more)

### Community 36 - "Voice.kt"
Cohesion: 0.11
Nodes (3): Recorder, SpeakItem, Voice

### Community 39 - "Updater.kt"
Cohesion: 0.33
Nodes (3): UpdateException, UpdateInfo, Updater

### Community 41 - "Reviewer.kt"
Cohesion: 0.22
Nodes (7): Review, ReviewConcern, Reviewer, ReviewLink, ReviewNode, ReviewParse, Langs

### Community 42 - "Repo.kt"
Cohesion: 0.12
Nodes (4): CallResult, Called, Dialer, Failed

### Community 48 - "TalkViewModel.kt"
Cohesion: 0.13
Nodes (8): TalkEvent, Done, OpenEmergency, TalkMode, Idle, Listening, Speaking, Thinking

### Community 49 - "Voice.kt"
Cohesion: 0.09
Nodes (10): RecognitionListener, VoiceFail, VoiceIssue, LangMissing, None, Unavailable, VoiceKind, Failed (+2 more)

### Community 50 - "LlmStore"
Cohesion: 0.15
Nodes (6): LlmStore, Escalation, AgentCall, AgentNotConfiguredException, SarvamVoiceAgent, VoiceAgent

### Community 52 - ".intake"
Cohesion: 0.18
Nodes (9): CareFacts, Checks, Flag, MealSkipped, MoodLow, PainUp, SleepShort, WaterHigh (+1 more)

### Community 54 - "LlmToolsTest.kt"
Cohesion: 0.12
Nodes (6): History, SentenceSplitter, EpState, Done, Timeout, Waiting

### Community 55 - ".run"
Cohesion: 0.14
Nodes (8): Agent, ChatApi, Msg, ParsedReply, ChatApi, ChatApi, ChatApi, ChatApi

### Community 56 - "SafeArea.kt"
Cohesion: 0.11
Nodes (4): Location, SafeArea, LocationListener, SafeMath

### Community 57 - "Open"
Cohesion: 0.15
Nodes (13): Open, AskMedicines, Checkin, Evening, Fall, Family, FamilyGeneric, Heat (+5 more)

### Community 59 - "WakeReq"
Cohesion: 0.21
Nodes (9): WakeReady, WakeReq, Autostart, Battery, Exact, FullScreen, Mic, Notifications (+1 more)

### Community 62 - "UpdateState.kt"
Cohesion: 0.17
Nodes (3): UpdateBanner(), UpdateState, UpdateUi

### Community 64 - "GNode"
Cohesion: 0.19
Nodes (3): GEdge, GNode, GraphRank

### Community 67 - ".addMemory"
Cohesion: 0.36
Nodes (3): Fact, Facts, FactsResult

### Community 68 - "Components.kt"
Cohesion: 0.05
Nodes (12): animMs(), Layout, pressScale(), ProvideLayout(), rememberLayout(), Cloud, OrbState, Idle (+4 more)

### Community 70 - "WakeReason"
Cohesion: 0.20
Nodes (9): WakeReason, Checkin, Evening, Fall, FamilyMessage, Heat, Medicine, Morning (+1 more)

### Community 71 - "DoseLog"
Cohesion: 0.20
Nodes (7): DoseLog, Medicine, DoseSlots, DueDose, nextDose(), todayDoses(), MedScheduleTest

### Community 72 - "ConversationPanel.kt"
Cohesion: 0.07
Nodes (8): SimhaApp, CaregiverHomeScreen(), eventLabel(), shortTime(), SignalDetail(), VoiceMessageCard(), line(), FadeWords()

### Community 74 - "CaregiverStats.kt"
Cohesion: 0.27
Nodes (8): Diary, TimeUtil, dayOf(), DayStats, dayStats(), WeekStats, weekStats(), CaregiverStatsTest

### Community 82 - "TaskType"
Cohesion: 0.18
Nodes (11): clear(), Form, Step, Ask, Done, TaskType, Intake, Medicine (+3 more)

### Community 88 - "parseTime"
Cohesion: 0.32
Nodes (4): isDuplicateMemory(), nextOccurrence(), parseTime(), ScheduleTest

### Community 92 - "RecPhase"
Cohesion: 0.50
Nodes (4): RecPhase, Idle, Ready, Recording

### Community 96 - "SarvamHealthTest"
Cohesion: 0.15
Nodes (3): HealthMemory, Endpointer, SarvamHealthTest

### Community 98 - "Work"
Cohesion: 0.21
Nodes (3): ReminderWorker, SyncWorker, Work

### Community 102 - "Wake.kt"
Cohesion: 0.24
Nodes (3): forLang(), toUi(), UiSettings

### Community 104 - "NodeType"
Cohesion: 0.20
Nodes (10): NodeType, Allergy, Concern, Condition, Event, Fact, Medicine, Person (+2 more)

### Community 120 - "Kind"
Cohesion: 0.40
Nodes (5): Kind, Emergency, Primary, Secondary, Success

### Community 126 - "SlotKind"
Cohesion: 0.33
Nodes (6): SlotKind, Checkin, Evening, Heat, Medicine, Morning

## Knowledge Gaps
- **126 isolated node(s):** `WaterLow`, `WaterHigh`, `SleepShort`, `PainUp`, `MoodLow` (+121 more)
  These have ≤1 connection - possible missing edges. (Counts symbols only; 408 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **94 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `jo()` connect `jo` to `Body`, `ContactInfo`, `TalkViewModel`, `Actions`, `Wake.kt`, `GuardianRepo`, `Query`, `Onboarding.kt`, `WakeWord.kt`, `Models.kt`, `.execute`, `MainViewModel`, `ActivityService.kt`, `Reviewer.kt`, `Repo.kt`, `.aux`, `TalkViewModel.kt`, `LlmStore`, `LlmToolsTest.kt`, `SafeArea.kt`, `CareEngine`, `.uid`, `ConversationPanel.kt`, `Wake.kt`?**
  _High betweenness centrality (0.072) - this node is a cross-community bridge._
- **Are the 26 inferred relationships involving `jo()` (e.g. with `.addMemory()` and `.deleteMedicine()`) actually correct?**
  _`jo()` has 26 INFERRED edges - model-reasoned connections that need verification._
- **What connects `WaterLow`, `WaterHigh`, `SleepShort` to the rest of the system?**
  _126 weakly-connected nodes found - possible missing edges._
- **Should `Body` be split into smaller, more focused modules?**
  _Cohesion score 0.13602693602693602 - nodes in this community are weakly interconnected._
- **Why does `ToolHost` connect `.execute` to `Body`, `ContactInfo`, `SarvamHealthTest`, `FakeHost`, `TalkViewModel.kt`, `LlmToolsTest.kt`, `.run`?**
  _High betweenness centrality (0.055) - this node is a cross-community bridge._
- **Should `ContactInfo` be split into smaller, more focused modules?**
  _Cohesion score 0.13043478260869565 - nodes in this community are weakly interconnected._
- **Why does `Repo` connect `jo` to `MainViewModel`, `Db.kt`, `.uid`, `Voice.kt`, `Repo.kt`, `GuardianRepo`, `Home.kt`, `TalkViewModel.kt`, `Query`, `Voice.kt`, `Onboarding.kt`, `SafeArea.kt`, `SimhaApp.kt`?**
  _High betweenness centrality (0.048) - this node is a cross-community bridge._