# Graph Report - Simha  (2026-11-24)

## Corpus Check
- 93 files · ~60,738 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 79 file(s) not represented in the graph (top: .xml 75, (none) 1, .ttf 1)
- WARNING: 79 of 180 files seen (44%) have no supported extractor and are missing from this graph entirely. It may look complete while representing only part of the codebase.

## Summary
- 1883 nodes · 5450 edges · 123 communities (47 shown, 76 thin omitted)
- Extraction: 94% EXTRACTED · 6% INFERRED · 0% AMBIGUOUS · INFERRED: 317 edges (avg confidence: 0.82)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `c7e0a3d6`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- Body
- Notifier
- Db.kt
- TalkViewModel
- Actions
- MedicalProfile
- PlansTest
- jo
- .decide
- with
- UpdateWorker.kt
- Wake
- Models.kt
- Onboarding.kt
- WakeMathTest
- Query
- Theme.kt
- Home.kt
- MainViewModel.kt
- SarvamConfig
- Sig
- LlmToolsTest.kt
- .stream
- .put
- .execute
- Situation
- MainViewModel
- OrbGlView
- .learn
- Perm
- Voice
- App.kt
- MainActivity.kt
- Updater.kt
- FakeHost
- SimhaApp.kt
- Repo.kt
- MsgRecorder
- GuardianTest
- Voice.kt
- .finish
- TalkViewModel.kt
- UsageReader.kt
- Escalation.kt
- UpdateInstallReceiver.kt
- .check
- SarvamHealthTest.kt
- ContactInfo
- SafeArea.kt
- Open
- Voice.kt
- WakeReq
- .envelope
- SimhaApp.kt
- UpdateState.kt
- ActivityService.kt
- .aux
- Weather.kt
- .uid
- .addMemory
- Components.kt
- OrbState
- WakeReason
- DoseLog
- FillerGate
- Oem
- nextOccurrence
- .status
- .listenSarvam
- Caregiver.kt
- HealthMemory
- AlarmReceiver.kt
- .load
- StepName
- Actions.kt
- LlmStore
- SlotKind
- Kind
- .chat

## God Nodes (most connected - your core abstractions)
1. `MainViewModel` - 72 edges
2. `jo()` - 60 edges
3. `Repo` - 50 edges
4. `FakeHost` - 45 edges
5. `Wake` - 41 edges
6. `Body()` - 38 edges
7. `ToolHost` - 35 edges
8. `TalkViewModel` - 33 edges
9. `Snap` - 32 edges
10. `Voice` - 32 edges

## Surprising Connections (you probably didn't know these)
- `SimhaOrb()` --calls--> `Stroke`  [INFERRED]
  app/src/main/java/com/simha/app/ui/home/SimhaOrb.kt → app/src/main/java/com/simha/app/logic/FirstAid.kt
- `Voice` --calls--> `SarvamClient`  [INFERRED]
  app/src/main/java/com/simha/app/voice/Voice.kt → app/src/main/java/com/simha/app/voice/SarvamClient.kt
- `ConversationPanel()` --calls--> `UiMsg`  [INFERRED]
  app/src/main/java/com/simha/app/ui/talk/ConversationPanel.kt → app/src/main/java/com/simha/app/ui/talk/TalkViewModel.kt
- `AppNav()` --calls--> `AuditScreen()`  [INFERRED]
  app/src/main/java/com/simha/app/ui/App.kt → app/src/main/java/com/simha/app/ui/Caregiver.kt
- `AppNav()` --calls--> `CaregiverHomeScreen()`  [INFERRED]
  app/src/main/java/com/simha/app/ui/App.kt → app/src/main/java/com/simha/app/ui/Caregiver.kt

## Import Cycles
- None detected.

## Communities (123 total, 76 thin omitted)

### Community 0 - "Body"
Cohesion: 0.12
Nodes (39): BigButton(), Body(), ButtonCore(), Caption(), Card(), Chip(), ChipGroup(), EmergencyButton() (+31 more)

### Community 2 - "Db.kt"
Cohesion: 0.13
Nodes (8): ContactEntity, FactEntity, MedicineEntity, OutboxEntity, ProfileEntity, SimhaDao, SimhaDb, WeeklySignalEntity

### Community 3 - "TalkViewModel"
Cohesion: 0.16
Nodes (3): ChatLine, TalkViewModel, UiMsg

### Community 5 - "MedicalProfile"
Cohesion: 0.15
Nodes (6): Guardian, GuardianContext, Known, MedLine, MedicalProfile, OpeningFillerTest

### Community 6 - "PlansTest"
Cohesion: 0.07
Nodes (25): Ago, Days, Hours, Minutes, Never, Now, Answer, Fine (+17 more)

### Community 8 - ".decide"
Cohesion: 0.16
Nodes (9): Approvals, Decision, AlertFamily, EscalateCallFamily, None, PromptCheckin, Rules, RulesEngine (+1 more)

### Community 9 - "with"
Cohesion: 0.06
Nodes (33): runs-on, steps, concurrency, cancel-in-progress, group, env, KEYSTORE_BASE64, VERSION_CODE (+25 more)

### Community 10 - "UpdateWorker.kt"
Cohesion: 0.12
Nodes (4): ActivityWorker, ReminderWorker, SyncWorker, UpdateWorker

### Community 12 - "Wake"
Cohesion: 0.15
Nodes (3): Wake, WakeAudio, WakeRequest

### Community 13 - "Models.kt"
Cohesion: 0.26
Nodes (11): AuditRow, CallRow, CaregiverRow, DailySignalRow, EventRow, FamilyMessage, RecordingRow, UserRow (+3 more)

### Community 15 - "Onboarding.kt"
Cohesion: 0.05
Nodes (12): OBK, fadeUp(), openMaps(), PermText, permText(), openMaps(), PickPhone, FadeWords() (+4 more)

### Community 16 - "WakeMathTest"
Cohesion: 0.25
Nodes (3): Slot, WakeMath, WakeMathTest

### Community 17 - "Query"
Cohesion: 0.13
Nodes (4): DailySignalEntity, GuardianDao, GuardianRepo, Memory

### Community 18 - "Theme.kt"
Cohesion: 0.09
Nodes (4): SimhaColors, nunito(), scheme(), SimhaTheme()

### Community 19 - "Home.kt"
Cohesion: 0.07
Nodes (15): TodayItem, TodayState, agoText(), ActionTile(), HomeScreen(), SimhaOrb(), TodayRow(), todayText() (+7 more)

### Community 20 - "MainViewModel.kt"
Cohesion: 0.06
Nodes (8): K, Prefs, toUi(), UiKeys, UiSettings, WakeWordEngine, RecognitionListener, WakeWordPrefs

### Community 21 - "SarvamConfig"
Cohesion: 0.22
Nodes (4): SarvamConfig, Sarvam, SarvamException, SarvamClient

### Community 22 - "Sig"
Cohesion: 0.10
Nodes (13): Diary, DiaryMerge, TimeUtil, Rollup, Sig, Signal, dayOf(), DayStats (+5 more)

### Community 23 - "LlmToolsTest.kt"
Cohesion: 0.13
Nodes (9): Agent, ChatApi, History, Msg, ParsedReply, SentenceSplitter, ToolCall, ChatApi (+1 more)

### Community 25 - ".stream"
Cohesion: 0.22
Nodes (6): LlmClient, LlmConfig, LlmException, LlmParse, StreamAcc, CallAcc

### Community 29 - ".execute"
Cohesion: 0.04
Nodes (4): SimhaTools, ToolHost, Host, HealthHost

### Community 31 - "Situation"
Cohesion: 0.11
Nodes (19): FirstAid, Guide, Situation, Allergy, Anaphylaxis, Bleeding, Breathless, Burn (+11 more)

### Community 32 - "MainViewModel"
Cohesion: 0.12
Nodes (11): Snap, MainViewModel, SectionTitle(), AboutPage(), CheckinsPage(), hoursText(), MePage(), minutesText() (+3 more)

### Community 33 - "OrbGlView"
Cohesion: 0.06
Nodes (9): FallDetector, FallMachine, Phase, Free, Idle, Impact, Gl, OrbGlView (+1 more)

### Community 34 - ".learn"
Cohesion: 0.20
Nodes (3): NightState, Routine, RoutineTest

### Community 35 - "Perm"
Cohesion: 0.19
Nodes (11): Perm, Battery, CallPhone, Camera, FullScreen, Mic, Notifications, Overlay (+3 more)

### Community 37 - "App.kt"
Cohesion: 0.09
Nodes (4): AppNav(), BottomBar(), SimhaRoot(), tabSwitch()

### Community 39 - "Updater.kt"
Cohesion: 0.15
Nodes (3): UpdateException, UpdateInfo, Updater

### Community 45 - "GuardianTest"
Cohesion: 0.16
Nodes (5): Scam, Sig, ScamReport, WakeWordMatcher, GuardianTest

### Community 46 - "Voice.kt"
Cohesion: 0.17
Nodes (6): RecognitionListener, VoiceFail, VoiceKind, Failed, NoSpeech, Unavailable

### Community 48 - "TalkViewModel.kt"
Cohesion: 0.12
Nodes (8): TalkEvent, Done, OpenEmergency, TalkMode, Idle, Listening, Speaking, Thinking

### Community 50 - "Escalation.kt"
Cohesion: 0.15
Nodes (9): CallResult, Called, Dialer, Failed, Escalation, AgentCall, AgentNotConfiguredException, SarvamVoiceAgent (+1 more)

### Community 52 - ".check"
Cohesion: 0.24
Nodes (3): Location, SafeArea, SafeMath

### Community 54 - "SarvamHealthTest.kt"
Cohesion: 0.09
Nodes (7): Endpointer, EpState, Done, Timeout, Waiting, Wav, SarvamHealthTest

### Community 57 - "Open"
Cohesion: 0.15
Nodes (13): Open, AskMedicines, Checkin, Evening, Fall, Family, FamilyGeneric, Heat (+5 more)

### Community 59 - "WakeReq"
Cohesion: 0.21
Nodes (9): WakeReady, WakeReq, Autostart, Battery, Exact, FullScreen, Mic, Notifications (+1 more)

### Community 61 - "SimhaApp.kt"
Cohesion: 0.29
Nodes (3): ServiceLocator, SimhaApp, Recorder

### Community 67 - ".addMemory"
Cohesion: 0.32
Nodes (3): Fact, Facts, FactsResult

### Community 68 - "Components.kt"
Cohesion: 0.05
Nodes (3): Layout, ProvideLayout(), rememberLayout()

### Community 69 - "OrbState"
Cohesion: 0.40
Nodes (5): OrbState, Idle, Listening, Speaking, Thinking

### Community 70 - "WakeReason"
Cohesion: 0.22
Nodes (9): WakeReason, Checkin, Evening, Fall, FamilyMessage, Heat, Medicine, Morning (+1 more)

### Community 71 - "DoseLog"
Cohesion: 0.24
Nodes (5): Medicine, DueDose, nextDose(), todayDoses(), MedScheduleTest

### Community 86 - "nextOccurrence"
Cohesion: 0.32
Nodes (4): isDuplicateMemory(), nextOccurrence(), parseTime(), ScheduleTest

### Community 97 - "Caregiver.kt"
Cohesion: 0.19
Nodes (14): AuditScreen(), CaregiverHomeScreen(), CaregiverSetupScreen(), eventLabel(), RecPhase, Idle, Ready, Recording (+6 more)

### Community 116 - "StepName"
Cohesion: 0.62
Nodes (6): ChoiceCard(), OnboardingScreen(), Progress(), StepName(), StepWho(), Up()

### Community 119 - "SlotKind"
Cohesion: 0.33
Nodes (6): SlotKind, Checkin, Evening, Heat, Medicine, Morning

### Community 120 - "Kind"
Cohesion: 0.40
Nodes (5): Kind, Emergency, Primary, Secondary, Success

## Knowledge Gaps
- **106 isolated node(s):** `Checkin`, `Medicine`, `Morning`, `Evening`, `Fall` (+101 more)
  These have ≤1 connection - possible missing edges. (Counts symbols only; 363 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **76 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `jo()` connect `jo` to `Body`, `TalkViewModel`, `Actions`, `.decide`, `UpdateWorker.kt`, `Wake`, `Models.kt`, `Onboarding.kt`, `Query`, `MainViewModel.kt`, `.execute`, `MainViewModel`, `OrbGlView`, `TalkViewModel.kt`, `Escalation.kt`, `.check`, `SafeArea.kt`, `.aux`, `.uid`, `.addMemory`, `Wake.kt`?**
  _High betweenness centrality (0.069) - this node is a cross-community bridge._
- **Are the 26 inferred relationships involving `jo()` (e.g. with `.addMemory()` and `.deleteMedicine()`) actually correct?**
  _`jo()` has 26 INFERRED edges - model-reasoned connections that need verification._
- **What connects `Checkin`, `Medicine`, `Morning` to the rest of the system?**
  _106 weakly-connected nodes found - possible missing edges._
- **Should `Body` be split into smaller, more focused modules?**
  _Cohesion score 0.11818181818181818 - nodes in this community are weakly interconnected._
- **Why does `Repo` connect `jo` to `MainViewModel`, `Db.kt`, `.uid`, `.decide`, `Repo.kt`, `Onboarding.kt`, `TalkViewModel.kt`, `Query`, `Home.kt`, `MainViewModel.kt`, `SafeArea.kt`, `Voice.kt`, `SimhaApp.kt`?**
  _High betweenness centrality (0.044) - this node is a cross-community bridge._
- **Should `Db.kt` be split into smaller, more focused modules?**
  _Cohesion score 0.13306451612903225 - nodes in this community are weakly interconnected._
- **Why does `MainViewModel` connect `MainViewModel` to `Body`, `Caregiver.kt`, `Db.kt`, `App.kt`, `MainActivity.kt`, `DoseLog`, `Onboarding.kt`, `Home.kt`, `MainViewModel.kt`, `StepName`, `.status`, `Voice.kt`?**
  _High betweenness centrality (0.037) - this node is a cross-community bridge._