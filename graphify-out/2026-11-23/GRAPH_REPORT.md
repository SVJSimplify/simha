# Graph Report - Simha  (2026-11-23)

## Corpus Check
- 90 files · ~57,075 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 74 file(s) not represented in the graph (top: .xml 70, (none) 1, .ttf 1)
- WARNING: 74 of 172 files seen (43%) have no supported extractor and are missing from this graph entirely. It may look complete while representing only part of the codebase.

## Summary
- 1792 nodes · 5239 edges · 88 communities (42 shown, 46 thin omitted)
- Extraction: 94% EXTRACTED · 6% INFERRED · 0% AMBIGUOUS · INFERRED: 315 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `1d678458`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- Body
- Notifier
- Repo
- TalkViewModel
- Actions
- MedicalProfile
- PlansTest
- Components.kt
- .decide
- with
- UpdateWorker.kt
- Updater.kt
- Wake
- MainViewModel
- Test
- Theme.kt
- Onboarding.kt
- MedicinesScreen
- SarvamConfig
- DoseLog
- LlmToolsTest.kt
- .stream
- SimhaApp.kt
- .execute
- MainViewModel.kt
- Situation
- Weather
- FallDetector.kt
- .learn
- CaregiverStats.kt
- Voice
- SafeArea.kt
- MainActivity.kt
- Test
- FakeHost
- SimhaApp.kt
- Monitor.kt
- MsgRecorder
- ActivityService.kt
- Voice.kt
- .finish
- TalkViewModel.kt
- WakeWord.kt
- VoiceAgent.kt
- Host
- Models.kt
- HealthMemory
- ContactInfo
- Caregiver.kt
- Open
- WakeWordEngine
- RecognitionListener
- .envelope
- RecPhase
- MedicalId.kt
- AlarmReceiver.kt
- StepName
- OrbShader
- WakeReason
- SlotKind
- FillerGate
- OrbState
- Context

## God Nodes (most connected - your core abstractions)
1. `MainViewModel` - 72 edges
2. `jo()` - 58 edges
3. `Repo` - 50 edges
4. `FakeHost` - 45 edges
5. `Wake` - 40 edges
6. `Body()` - 38 edges
7. `ToolHost` - 35 edges
8. `TalkViewModel` - 34 edges
9. `CheckinsPage()` - 32 edges
10. `Voice` - 32 edges

## Surprising Connections (you probably didn't know these)
- `SimhaOrb()` --calls--> `Stroke`  [INFERRED]
  app/src/main/java/com/simha/app/ui/home/SimhaOrb.kt → app/src/main/java/com/simha/app/logic/FirstAid.kt
- `SimhaRoot()` --calls--> `ProvideLayout()`  [INFERRED]
  app/src/main/java/com/simha/app/ui/App.kt → app/src/main/java/com/simha/app/ui/Components.kt
- `AppNav()` --calls--> `animMs()`  [INFERRED]
  app/src/main/java/com/simha/app/ui/App.kt → app/src/main/java/com/simha/app/ui/Components.kt
- `AppNav()` --calls--> `HealthScreen()`  [INFERRED]
  app/src/main/java/com/simha/app/ui/App.kt → app/src/main/java/com/simha/app/ui/Health.kt
- `AppNav()` --calls--> `HomeScreen()`  [INFERRED]
  app/src/main/java/com/simha/app/ui/App.kt → app/src/main/java/com/simha/app/ui/Home.kt

## Import Cycles
- None detected.

## Communities (88 total, 46 thin omitted)

### Community 0 - "Body"
Cohesion: 0.12
Nodes (33): agoText(), AnimatedEntrance(), animMs(), BigButton(), Body(), ButtonCore(), Caption(), Chip() (+25 more)

### Community 2 - "Repo"
Cohesion: 0.06
Nodes (16): ContactEntity, DiaryEntity, DoseLogEntity, GuardianDao, MedicineEntity, MemoryEntity, OutboxEntity, ProfileEntity (+8 more)

### Community 3 - "TalkViewModel"
Cohesion: 0.17
Nodes (3): ChatLine, TalkViewModel, UiMsg

### Community 4 - "Actions"
Cohesion: 0.06
Nodes (25): Actions, Perm, Battery, CallPhone, Camera, FullScreen, Mic, Notifications (+17 more)

### Community 5 - "MedicalProfile"
Cohesion: 0.16
Nodes (7): Guardian, GuardianContext, Known, MedLine, MedicalProfile, Langs, OpeningFillerTest

### Community 6 - "PlansTest"
Cohesion: 0.07
Nodes (25): Ago, Days, Hours, Minutes, Never, Now, Answer, Fine (+17 more)

### Community 7 - "Components.kt"
Cohesion: 0.04
Nodes (7): Kind, Emergency, Primary, Secondary, Success, ProvideLayout(), rememberLayout()

### Community 8 - ".decide"
Cohesion: 0.16
Nodes (9): Approvals, Decision, AlertFamily, EscalateCallFamily, None, PromptCheckin, Rules, RulesEngine (+1 more)

### Community 9 - "with"
Cohesion: 0.06
Nodes (33): runs-on, steps, concurrency, cancel-in-progress, group, env, KEYSTORE_BASE64, VERSION_CODE (+25 more)

### Community 10 - "UpdateWorker.kt"
Cohesion: 0.12
Nodes (4): ActivityWorker, ReminderWorker, SyncWorker, UpdateWorker

### Community 11 - "Updater.kt"
Cohesion: 0.05
Nodes (6): UpdateInstallReceiver, UpdateException, UpdateInfo, Updater, UpdateState, UpdateUi

### Community 12 - "Wake"
Cohesion: 0.06
Nodes (10): Location, Monitor, SafeArea, UsageReader, Wake, WakeAudio, WakeActivity, RoutineStore (+2 more)

### Community 15 - "MainViewModel"
Cohesion: 0.12
Nodes (14): Snap, MainViewModel, TodayItem, TodayState, SectionTitle(), AboutPage(), CheckinsPage(), hoursText() (+6 more)

### Community 18 - "Theme.kt"
Cohesion: 0.09
Nodes (4): SimhaColors, nunito(), scheme(), SimhaTheme()

### Community 19 - "Onboarding.kt"
Cohesion: 0.05
Nodes (4): OBK, Layout, fadeUp(), TalkScreen()

### Community 20 - "MedicinesScreen"
Cohesion: 0.31
Nodes (6): doseStatusText(), MedicineForm(), MedicinesScreen(), showTime(), TimeDialog(), TimeTag()

### Community 21 - "SarvamConfig"
Cohesion: 0.20
Nodes (4): SarvamConfig, Sarvam, SarvamException, SarvamClient

### Community 22 - "DoseLog"
Cohesion: 0.24
Nodes (7): DoseLog, Medicine, DoseSlots, DueDose, nextDose(), todayDoses(), MedScheduleTest

### Community 23 - "LlmToolsTest.kt"
Cohesion: 0.11
Nodes (10): Agent, ChatApi, History, Msg, ParsedReply, SentenceSplitter, ToolCall, ChatApi (+2 more)

### Community 25 - ".stream"
Cohesion: 0.22
Nodes (6): LlmClient, LlmConfig, LlmException, LlmParse, StreamAcc, CallAcc

### Community 26 - "SimhaApp.kt"
Cohesion: 0.16
Nodes (4): LlmStore, ServiceLocator, SimhaApp, Recorder

### Community 29 - ".execute"
Cohesion: 0.05
Nodes (4): SimhaTools, ToolHost, HealthHost, SarvamHealthTest

### Community 30 - "MainViewModel.kt"
Cohesion: 0.15
Nodes (3): toUi(), UiKeys, UiSettings

### Community 31 - "Situation"
Cohesion: 0.06
Nodes (25): DiaryMerge, Scam, Sig, ScamReport, FirstAid, Guide, Situation, Allergy (+17 more)

### Community 32 - "Weather"
Cohesion: 0.14
Nodes (5): Weather, WeatherReport, Slot, WakeMath, WakeMathTest

### Community 33 - "FallDetector.kt"
Cohesion: 0.11
Nodes (8): FallDetector, FallMachine, Phase, Free, Idle, Impact, SafeMath, FallMachineTest

### Community 34 - ".learn"
Cohesion: 0.20
Nodes (3): NightState, Routine, RoutineTest

### Community 35 - "CaregiverStats.kt"
Cohesion: 0.24
Nodes (9): Diary, TimeUtil, dayOf(), DayStats, dayStats(), text(), WeekStats, weekStats() (+1 more)

### Community 39 - "Test"
Cohesion: 0.30
Nodes (4): isDuplicateMemory(), nextOccurrence(), parseTime(), ScheduleTest

### Community 42 - "Monitor.kt"
Cohesion: 0.07
Nodes (5): EpState, Done, Timeout, Waiting, Wav

### Community 46 - "Voice.kt"
Cohesion: 0.09
Nodes (10): RecognitionListener, VoiceFail, VoiceIssue, LangMissing, None, Unavailable, VoiceKind, Failed (+2 more)

### Community 48 - "TalkViewModel.kt"
Cohesion: 0.14
Nodes (8): TalkEvent, Done, OpenEmergency, TalkMode, Idle, Listening, Speaking, Thinking

### Community 50 - "VoiceAgent.kt"
Cohesion: 0.15
Nodes (9): CallResult, Called, Dialer, Failed, Escalation, AgentCall, AgentNotConfiguredException, SarvamVoiceAgent (+1 more)

### Community 52 - "Models.kt"
Cohesion: 0.38
Nodes (10): AuditRow, CallRow, CaregiverRow, DiaryRow, DoseRow, EventRow, FamilyMessage, RecordingRow (+2 more)

### Community 56 - "Caregiver.kt"
Cohesion: 0.14
Nodes (20): AppNav(), BottomBar(), SimhaRoot(), tabSwitch(), AuditScreen(), CaregiverHomeScreen(), CaregiverSetupScreen(), eventLabel() (+12 more)

### Community 57 - "Open"
Cohesion: 0.15
Nodes (13): Open, AskMedicines, Checkin, Evening, Fall, Family, FamilyGeneric, Heat (+5 more)

### Community 61 - "RecPhase"
Cohesion: 0.50
Nodes (4): RecPhase, Idle, Ready, Recording

### Community 62 - "MedicalId.kt"
Cohesion: 0.30
Nodes (6): LabeledField(), roleLabel(), line(), MedicalIdNotif, MedicalIdScreen(), medLine()

### Community 67 - "StepName"
Cohesion: 0.62
Nodes (6): ChoiceCard(), OnboardingScreen(), Progress(), StepName(), StepWho(), Up()

### Community 70 - "WakeReason"
Cohesion: 0.22
Nodes (9): WakeReason, Checkin, Evening, Fall, FamilyMessage, Heat, Medicine, Morning (+1 more)

### Community 71 - "SlotKind"
Cohesion: 0.33
Nodes (6): SlotKind, Checkin, Evening, Heat, Medicine, Morning

### Community 76 - "OrbState"
Cohesion: 0.40
Nodes (5): OrbState, Idle, Listening, Speaking, Thinking

## Knowledge Gaps
- **106 isolated node(s):** `Notifications`, `Mic`, `Camera`, `Usage`, `FullScreen` (+101 more)
  These have ≤1 connection - possible missing edges. (Counts symbols only; 332 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **46 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `jo()` connect `Repo` to `.getProfile`, `FallDetector.kt`, `TalkViewModel`, `Actions`, `SafeArea.kt`, `Wake.kt`, `.decide`, `UpdateWorker.kt`, `Wake`, `MainViewModel`, `TalkViewModel.kt`, `VoiceAgent.kt`, `Onboarding.kt`, `Models.kt`, `Host`, `Caregiver.kt`, `.execute`, `MainViewModel.kt`?**
  _High betweenness centrality (0.076) - this node is a cross-community bridge._
- **Are the 23 inferred relationships involving `jo()` (e.g. with `.addMemory()` and `.deleteMedicine()`) actually correct?**
  _`jo()` has 23 INFERRED edges - model-reasoned connections that need verification._
- **What connects `Notifications`, `Mic`, `Camera` to the rest of the system?**
  _106 weakly-connected nodes found - possible missing edges._
- **Should `Body` be split into smaller, more focused modules?**
  _Cohesion score 0.12056737588652482 - nodes in this community are weakly interconnected._
- **Why does `Repo` connect `Repo` to `SafeArea.kt`, `.decide`, `Monitor.kt`, `Voice.kt`, `MainViewModel`, `TalkViewModel.kt`, `WakeWord.kt`, `Onboarding.kt`, `SimhaApp.kt`?**
  _High betweenness centrality (0.057) - this node is a cross-community bridge._
- **Should `Repo` be split into smaller, more focused modules?**
  _Cohesion score 0.058613659531090725 - nodes in this community are weakly interconnected._
- **Why does `MainViewModel` connect `MainViewModel` to `Body`, `Repo`, `StepName`, `MainActivity.kt`, `Onboarding.kt`, `MedicinesScreen`, `DoseLog`, `Caregiver.kt`, `MedicalId.kt`, `MainViewModel.kt`?**
  _High betweenness centrality (0.049) - this node is a cross-community bridge._