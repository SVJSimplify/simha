# Graph Report - Simha  (2026-11-25)

## Corpus Check
- 94 files · ~60,691 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 79 file(s) not represented in the graph (top: .xml 75, (none) 1, .ttf 1)
- WARNING: 79 of 181 files seen (44%) have no supported extractor and are missing from this graph entirely. It may look complete while representing only part of the codebase.

## Summary
- 1917 nodes · 5524 edges · 127 communities (47 shown, 80 thin omitted)
- Extraction: 94% EXTRACTED · 6% INFERRED · 0% AMBIGUOUS · INFERRED: 305 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `25f14153`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- HomeScreen
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
- Onboarding.kt
- Weather.kt
- Query
- Theme.kt
- Screens.kt
- WakeWord.kt
- SarvamConfig
- Sig
- .run
- .stream
- .execute
- .execute
- Situation
- MainViewModel
- ActivityService.kt
- .learn
- Perm
- Voice.kt
- .wrapped
- Updater.kt
- FakeHost
- Escalation.kt
- MsgRecorder
- WakeRequest
- GuardianTest
- Voice.kt
- .finish
- TalkViewModel.kt
- Voice.kt
- LlmStore
- UpdateInstallReceiver.kt
- .check
- SarvamHealthTest.kt
- coroutinescope
- SafeArea.kt
- Open
- WakeReq
- .envelope
- SimhaApp.kt
- UpdateState.kt
- dispatchers
- .uid
- .addMemory
- Components.kt
- WakeReason
- zoneid
- MedicalId.kt
- Oem
- MedicinesScreen
- .pollFamily
- FirstAid
- .resolve
- UsageReader.kt
- .toSig
- RecPhase
- SarvamHealthTest
- StepName
- Kind

## God Nodes (most connected - your core abstractions)
1. `MainViewModel` - 72 edges
2. `jo()` - 60 edges
3. `FakeHost` - 51 edges
4. `Repo` - 50 edges
5. `Wake` - 41 edges
6. `Body()` - 37 edges
7. `ToolHost` - 37 edges
8. `LlmToolsTest` - 37 edges
9. `TalkViewModel` - 33 edges
10. `GuardianRepo` - 32 edges

## Surprising Connections (you probably didn't know these)
- `SimhaRoot()` --calls--> `ProvideLayout()`  [INFERRED]
  app/src/main/java/com/simha/app/ui/App.kt → app/src/main/java/com/simha/app/ui/Components.kt
- `AppNav()` --calls--> `AuditScreen()`  [INFERRED]
  app/src/main/java/com/simha/app/ui/App.kt → app/src/main/java/com/simha/app/ui/Caregiver.kt
- `AppNav()` --calls--> `CaregiverHomeScreen()`  [INFERRED]
  app/src/main/java/com/simha/app/ui/App.kt → app/src/main/java/com/simha/app/ui/Caregiver.kt
- `AppNav()` --calls--> `CaregiverSetupScreen()`  [INFERRED]
  app/src/main/java/com/simha/app/ui/App.kt → app/src/main/java/com/simha/app/ui/Caregiver.kt
- `AppNav()` --calls--> `animMs()`  [INFERRED]
  app/src/main/java/com/simha/app/ui/App.kt → app/src/main/java/com/simha/app/ui/Components.kt

## Import Cycles
- None detected.

## Communities (127 total, 80 thin omitted)

### Community 0 - "HomeScreen"
Cohesion: 0.15
Nodes (27): TodayItem, TodayState, agoText(), AnimatedEntrance(), animMs(), BigButton(), ButtonCore(), Chip() (+19 more)

### Community 2 - "Db.kt"
Cohesion: 0.11
Nodes (8): ContactEntity, FactEntity, MedicineEntity, OutboxEntity, ProfileEntity, SimhaDao, SimhaDb, ServiceLocator

### Community 3 - "TalkViewModel"
Cohesion: 0.16
Nodes (5): ConversationPanel(), TalkScreen(), ChatLine, TalkViewModel, UiMsg

### Community 5 - "MedicalProfile"
Cohesion: 0.17
Nodes (7): ContactLine, Guardian, GuardianContext, Known, MedLine, MedicalProfile, OpeningFillerTest

### Community 6 - "PlansTest"
Cohesion: 0.07
Nodes (26): Ago, Days, Hours, Minutes, Never, Now, Answer, Fine (+18 more)

### Community 7 - "jo"
Cohesion: 0.25
Nodes (3): jo(), Prefs, Repo

### Community 8 - ".decide"
Cohesion: 0.16
Nodes (9): Approvals, Decision, AlertFamily, EscalateCallFamily, None, PromptCheckin, Rules, RulesEngine (+1 more)

### Community 9 - "with"
Cohesion: 0.06
Nodes (33): runs-on, steps, concurrency, cancel-in-progress, group, env, KEYSTORE_BASE64, VERSION_CODE (+25 more)

### Community 10 - "UpdateWorker.kt"
Cohesion: 0.06
Nodes (6): Notifier, ActivityWorker, ReminderWorker, SyncWorker, Work, UpdateWorker

### Community 12 - "Wake.kt"
Cohesion: 0.14
Nodes (4): toUi(), UiSettings, Wake, WakeAudio

### Community 15 - "Onboarding.kt"
Cohesion: 0.04
Nodes (3): OBK, Layout, FadeWords()

### Community 17 - "Query"
Cohesion: 0.21
Nodes (3): DailySignalEntity, GuardianDao, WeeklySignalEntity

### Community 18 - "Theme.kt"
Cohesion: 0.05
Nodes (5): MainActivity, SimhaColors, nunito(), scheme(), SimhaTheme()

### Community 19 - "Screens.kt"
Cohesion: 0.16
Nodes (12): AppNav(), BottomBar(), tabSwitch(), normalizeContactPhone(), ContactCard(), ContactsScreen(), EmergencyScreen(), openMaps() (+4 more)

### Community 20 - "WakeWord.kt"
Cohesion: 0.06
Nodes (7): K, UiKeys, WakeWord, WakeWordEngine, RecognitionListener, WakeWordMatcher, WakeWordPrefs

### Community 21 - "SarvamConfig"
Cohesion: 0.19
Nodes (4): SarvamConfig, Sarvam, SarvamException, SarvamClient

### Community 22 - "Sig"
Cohesion: 0.09
Nodes (22): AuditRow, CallRow, CaregiverRow, DailySignalRow, Diary, EventRow, FamilyMessage, RecordingRow (+14 more)

### Community 23 - ".run"
Cohesion: 0.15
Nodes (4): Agent, History, Msg, SentenceSplitter

### Community 25 - ".stream"
Cohesion: 0.20
Nodes (8): LlmClient, LlmConfig, LlmException, LlmParse, ParsedReply, StreamAcc, CallAcc, ToolCall

### Community 29 - ".execute"
Cohesion: 0.04
Nodes (3): ToolHost, Host, HealthHost

### Community 31 - "Situation"
Cohesion: 0.11
Nodes (18): Guide, Situation, Allergy, Anaphylaxis, Bleeding, Breathless, Burn, ChestPain (+10 more)

### Community 32 - "MainViewModel"
Cohesion: 0.12
Nodes (26): Snap, MainViewModel, AuditScreen(), CaregiverHomeScreen(), CaregiverSetupScreen(), eventLabel(), shortTime(), SignalDetail() (+18 more)

### Community 33 - "ActivityService.kt"
Cohesion: 0.08
Nodes (8): ActivityService, FallDetector, FallMachine, Phase, Free, Idle, Impact, FallMachineTest

### Community 34 - ".learn"
Cohesion: 0.20
Nodes (3): NightState, Routine, RoutineTest

### Community 35 - "Perm"
Cohesion: 0.18
Nodes (12): Perm, Battery, CallPhone, Camera, FullScreen, Mic, Notifications, Overlay (+4 more)

### Community 36 - "Voice.kt"
Cohesion: 0.16
Nodes (3): Recorder, SpeakItem, Voice

### Community 37 - ".wrapped"
Cohesion: 0.25
Nodes (3): LocaleCtx, LocalizedContext, SimhaRoot()

### Community 39 - "Updater.kt"
Cohesion: 0.33
Nodes (3): UpdateException, UpdateInfo, Updater

### Community 41 - "Escalation.kt"
Cohesion: 0.18
Nodes (6): CallResult, Called, Dialer, Failed, Escalation, Langs

### Community 45 - "GuardianTest"
Cohesion: 0.19
Nodes (4): Scam, Sig, ScamReport, GuardianTest

### Community 46 - "Voice.kt"
Cohesion: 0.14
Nodes (6): RecognitionListener, VoiceFail, VoiceKind, Failed, NoSpeech, Unavailable

### Community 48 - "TalkViewModel.kt"
Cohesion: 0.12
Nodes (9): FillerGate, TalkEvent, Done, OpenEmergency, TalkMode, Idle, Listening, Speaking (+1 more)

### Community 49 - "Voice.kt"
Cohesion: 0.12
Nodes (4): VoiceIssue, LangMissing, None, Unavailable

### Community 50 - "LlmStore"
Cohesion: 0.18
Nodes (5): LlmStore, AgentCall, AgentNotConfiguredException, SarvamVoiceAgent, VoiceAgent

### Community 55 - "coroutinescope"
Cohesion: 0.20
Nodes (5): ChatApi, ChatApi, ChatApi, ChatApi, ChatApi

### Community 57 - "Open"
Cohesion: 0.15
Nodes (13): Open, AskMedicines, Checkin, Evening, Fall, Family, FamilyGeneric, Heat (+5 more)

### Community 59 - "WakeReq"
Cohesion: 0.16
Nodes (10): forLang(), WakeReady, WakeReq, Autostart, Battery, Exact, FullScreen, Mic (+2 more)

### Community 67 - ".addMemory"
Cohesion: 0.36
Nodes (3): Fact, Facts, FactsResult

### Community 68 - "Components.kt"
Cohesion: 0.05
Nodes (9): ProvideLayout(), rememberLayout(), Cloud, OrbState, Idle, Listening, Speaking, Thinking (+1 more)

### Community 70 - "WakeReason"
Cohesion: 0.22
Nodes (9): WakeReason, Checkin, Evening, Fall, FamilyMessage, Heat, Medicine, Morning (+1 more)

### Community 71 - "zoneid"
Cohesion: 0.07
Nodes (20): DoseLog, Medicine, DoseSlots, isDuplicateMemory(), nextOccurrence(), parseTime(), DueDose, nextDose() (+12 more)

### Community 72 - "MedicalId.kt"
Cohesion: 0.27
Nodes (6): LabeledField(), roleLabel(), line(), MedicalIdNotif, MedicalIdScreen(), medLine()

### Community 77 - "MedicinesScreen"
Cohesion: 0.26
Nodes (6): doseStatusText(), MedicineForm(), MedicinesScreen(), showTime(), TimeDialog(), TimeTag()

### Community 92 - "RecPhase"
Cohesion: 0.50
Nodes (4): RecPhase, Idle, Ready, Recording

### Community 96 - "SarvamHealthTest"
Cohesion: 0.12
Nodes (7): HealthMemory, Endpointer, EpState, Done, Timeout, Waiting, SarvamHealthTest

### Community 97 - "StepName"
Cohesion: 0.42
Nodes (7): ChoiceCard(), fadeUp(), OnboardingScreen(), Progress(), StepName(), StepWho(), Up()

### Community 120 - "Kind"
Cohesion: 0.40
Nodes (5): Kind, Emergency, Primary, Secondary, Success

## Knowledge Gaps
- **107 isolated node(s):** `Checkin`, `Medicine`, `Morning`, `Evening`, `Fall` (+102 more)
  These have ≤1 connection - possible missing edges. (Counts symbols only; 369 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **80 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `jo()` connect `jo` to `ContactInfo`, `TalkViewModel`, `Actions`, `.decide`, `UpdateWorker.kt`, `Wake.kt`, `GuardianRepo`, `Onboarding.kt`, `Query`, `Screens.kt`, `WakeWord.kt`, `Sig`, `.execute`, `MainViewModel`, `ActivityService.kt`, `Escalation.kt`, `WakeRequest`, `TalkViewModel.kt`, `.check`, `SafeArea.kt`, `.uid`, `.addMemory`?**
  _High betweenness centrality (0.083) - this node is a cross-community bridge._
- **Are the 26 inferred relationships involving `jo()` (e.g. with `.addMemory()` and `.deleteMedicine()`) actually correct?**
  _`jo()` has 26 INFERRED edges - model-reasoned connections that need verification._
- **What connects `Checkin`, `Medicine`, `Morning` to the rest of the system?**
  _107 weakly-connected nodes found - possible missing edges._
- **Should `HomeScreen` be split into smaller, more focused modules?**
  _Cohesion score 0.14962121212121213 - nodes in this community are weakly interconnected._
- **Why does `Repo` connect `jo` to `MainViewModel`, `.uid`, `Db.kt`, `Voice.kt`, `.decide`, `Repo.kt`, `GuardianRepo`, `Onboarding.kt`, `TalkViewModel.kt`, `Voice.kt`, `Screens.kt`, `SafeArea.kt`, `SimhaApp.kt`?**
  _High betweenness centrality (0.054) - this node is a cross-community bridge._
- **Should `ContactInfo` be split into smaller, more focused modules?**
  _Cohesion score 0.14761904761904762 - nodes in this community are weakly interconnected._
- **Why does `ToolHost` connect `.execute` to `SarvamHealthTest`, `ContactInfo`, `FakeHost`, `MedicinesScreen`, `TalkViewModel.kt`, `SarvamHealthTest.kt`, `.run`, `.execute`?**
  _High betweenness centrality (0.044) - this node is a cross-community bridge._