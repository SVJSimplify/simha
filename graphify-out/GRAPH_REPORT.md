# Graph Report - Simha  (2026-11-23)

## Corpus Check
- 85 files · ~52,447 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 57 file(s) not represented in the graph (top: .xml 53, (none) 1, .ttf 1)
- WARNING: 57 of 150 files seen (38%) have no supported extractor and are missing from this graph entirely. It may look complete while representing only part of the codebase.

## Summary
- 1681 nodes · 4833 edges · 93 communities (41 shown, 52 thin omitted)
- Extraction: 95% EXTRACTED · 5% INFERRED · 0% AMBIGUOUS · INFERRED: 249 edges (avg confidence: 0.83)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `8f8a5a50`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- MainViewModel
- Notifier
- Repo
- TalkViewModel
- Perm
- MedicalProfile
- PlansTest
- Components.kt
- .decide
- with
- UpdateWorker.kt
- Updater.kt
- Wake
- LlmToolsTest.kt
- MainViewModel
- SarvamHealthTest.kt
- App.kt
- Onboarding.kt
- MedicinesScreen.kt
- SarvamConfig
- DoseLog
- build
- HomeScreen
- HealthHost
- ToolHost
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
- MsgRecorder
- .check
- Voice.kt
- .finish
- TalkViewModel.kt
- WakeReq
- VoiceAgent.kt
- Host
- Models.kt
- Actions
- HealthMemory
- ContactInfo
- Screens.kt
- .execute
- .conversation
- Kind
- RecPhase
- Context
- StepId
- WakeReason
- CoroutineWorker
- Plans.kt
- TalkMode
- SimhaColors
- .chat

## God Nodes (most connected - your core abstractions)
1. `MainViewModel` - 65 edges
2. `jo()` - 58 edges
3. `Repo` - 50 edges
4. `SettingsScreen()` - 46 edges
5. `FakeHost` - 43 edges
6. `Wake` - 40 edges
7. `ToolHost` - 34 edges
8. `TalkViewModel` - 33 edges
9. `Body()` - 32 edges
10. `HomeScreen()` - 31 edges

## Surprising Connections (you probably didn't know these)
- `TodayRow()` --calls--> `Body()`  [INFERRED]
  app/src/main/java/com/simha/app/ui/Home.kt → app/src/main/java/com/simha/app/ui/Components.kt
- `TodayRow()` --calls--> `Caption()`  [INFERRED]
  app/src/main/java/com/simha/app/ui/Home.kt → app/src/main/java/com/simha/app/ui/Components.kt
- `AppNav()` --calls--> `HomeScreen()`  [INFERRED]
  app/src/main/java/com/simha/app/ui/App.kt → app/src/main/java/com/simha/app/ui/Home.kt
- `HomeScreen()` --calls--> `Body()`  [INFERRED]
  app/src/main/java/com/simha/app/ui/Home.kt → app/src/main/java/com/simha/app/ui/Components.kt
- `HomeScreen()` --calls--> `Caption()`  [INFERRED]
  app/src/main/java/com/simha/app/ui/Home.kt → app/src/main/java/com/simha/app/ui/Components.kt

## Import Cycles
- None detected.

## Communities (93 total, 52 thin omitted)

### Community 0 - "MainViewModel"
Cohesion: 0.11
Nodes (36): Snap, AppNav(), AuditScreen(), CaregiverHomeScreen(), CaregiverSetupScreen(), eventLabel(), shortTime(), VoiceMessageCard() (+28 more)

### Community 2 - "Repo"
Cohesion: 0.05
Nodes (20): ContactEntity, DiaryEntity, DoseLogEntity, GuardianDao, MedicineEntity, MemoryEntity, OutboxEntity, ProfileEntity (+12 more)

### Community 3 - "TalkViewModel"
Cohesion: 0.21
Nodes (3): ConversationPanel(), TalkScreen(), TalkViewModel

### Community 4 - "Perm"
Cohesion: 0.17
Nodes (11): Perm, Battery, CallPhone, Camera, FullScreen, Mic, Notifications, Overlay (+3 more)

### Community 5 - "MedicalProfile"
Cohesion: 0.18
Nodes (5): Guardian, GuardianContext, MedLine, MedicalProfile, Langs

### Community 7 - "Components.kt"
Cohesion: 0.03
Nodes (10): Stroke, Layout, ProvideLayout(), rememberLayout(), OrbState, Idle, Listening, Speaking (+2 more)

### Community 8 - ".decide"
Cohesion: 0.17
Nodes (9): Approvals, Decision, AlertFamily, EscalateCallFamily, None, PromptCheckin, Rules, RulesEngine (+1 more)

### Community 9 - "with"
Cohesion: 0.06
Nodes (33): runs-on, steps, concurrency, cancel-in-progress, group, env, KEYSTORE_BASE64, VERSION_CODE (+25 more)

### Community 11 - "Updater.kt"
Cohesion: 0.05
Nodes (6): UpdateInstallReceiver, UpdateException, UpdateInfo, Updater, UpdateState, UpdateUi

### Community 12 - "Wake"
Cohesion: 0.07
Nodes (8): Monitor, UsageReader, Wake, WakeAudio, WakeActivity, RoutineStore, WakeRequest, WakeStore

### Community 15 - "MainViewModel"
Cohesion: 0.12
Nodes (9): MainViewModel, ChoiceCard(), OnboardingScreen(), Progress(), StepName(), StepWho(), hoursText(), minutesText() (+1 more)

### Community 17 - "SarvamHealthTest.kt"
Cohesion: 0.12
Nodes (6): Endpointer, EpState, Done, Timeout, Waiting, Wav

### Community 18 - "App.kt"
Cohesion: 0.07
Nodes (3): SimhaRoot(), scheme(), SimhaTheme()

### Community 19 - "Onboarding.kt"
Cohesion: 0.12
Nodes (7): openMaps(), PermText, permText(), VoiceIssue, LangMissing, None, Unavailable

### Community 20 - "MedicinesScreen.kt"
Cohesion: 0.19
Nodes (9): SectionTitle(), TextLink(), doseStatusText(), MedicineForm(), MedicinesScreen(), showMillis(), showTime(), TimeDialog() (+1 more)

### Community 21 - "SarvamConfig"
Cohesion: 0.17
Nodes (4): SarvamConfig, Sarvam, SarvamException, SarvamClient

### Community 22 - "DoseLog"
Cohesion: 0.25
Nodes (6): DoseLog, DoseSlots, DueDose, nextDose(), todayDoses(), MedScheduleTest

### Community 25 - "HomeScreen"
Cohesion: 0.18
Nodes (13): Ago, Days, Hours, Minutes, Never, Now, TodayItem, TodayState (+5 more)

### Community 30 - "MainViewModel.kt"
Cohesion: 0.05
Nodes (10): K, OBK, toUi(), UiKeys, UiSettings, WakeWord, WakeWordEngine, RecognitionListener (+2 more)

### Community 31 - "Situation"
Cohesion: 0.07
Nodes (20): DiaryMerge, FirstAid, Guide, Situation, Allergy, Anaphylaxis, Bleeding, Breathless (+12 more)

### Community 32 - "Weather"
Cohesion: 0.11
Nodes (11): Weather, WeatherReport, Slot, SlotKind, Checkin, Evening, Heat, Medicine (+3 more)

### Community 33 - "FallDetector.kt"
Cohesion: 0.07
Nodes (11): forLang(), ActivityService, FallDetector, FallMachine, Phase, Free, Idle, Impact (+3 more)

### Community 34 - ".learn"
Cohesion: 0.20
Nodes (3): NightState, Routine, RoutineTest

### Community 35 - "CaregiverStats.kt"
Cohesion: 0.24
Nodes (9): Diary, TimeUtil, dayOf(), DayStats, dayStats(), text(), WeekStats, weekStats() (+1 more)

### Community 39 - "Test"
Cohesion: 0.30
Nodes (4): isDuplicateMemory(), nextOccurrence(), parseTime(), ScheduleTest

### Community 40 - "FakeHost"
Cohesion: 0.07
Nodes (15): LlmClient, LlmConfig, History, LlmException, LlmParse, Msg, ParsedReply, SentenceSplitter (+7 more)

### Community 45 - ".check"
Cohesion: 0.24
Nodes (3): Location, SafeArea, SafeMath

### Community 46 - "Voice.kt"
Cohesion: 0.09
Nodes (6): RecognitionListener, VoiceFail, VoiceKind, Failed, NoSpeech, Unavailable

### Community 48 - "TalkViewModel.kt"
Cohesion: 0.12
Nodes (9): CallResult, Called, Dialer, Failed, Agent, ChatApi, TalkEvent, Done (+1 more)

### Community 49 - "WakeReq"
Cohesion: 0.19
Nodes (9): WakeReady, WakeReq, Autostart, Battery, Exact, FullScreen, Mic, Notifications (+1 more)

### Community 50 - "VoiceAgent.kt"
Cohesion: 0.15
Nodes (6): LlmStore, Escalation, AgentCall, AgentNotConfiguredException, SarvamVoiceAgent, VoiceAgent

### Community 52 - "Models.kt"
Cohesion: 0.38
Nodes (10): AuditRow, CallRow, CaregiverRow, DiaryRow, DoseRow, EventRow, FamilyMessage, RecordingRow (+2 more)

### Community 56 - "Screens.kt"
Cohesion: 0.21
Nodes (6): Step, openMaps(), PickPhone, ProfileScreen(), rememberPicker(), stepText()

### Community 58 - ".execute"
Cohesion: 0.22
Nodes (4): Scam, Sig, ScamReport, SimhaTools

### Community 60 - "Kind"
Cohesion: 0.40
Nodes (5): Kind, Emergency, Primary, Secondary, Success

### Community 61 - "RecPhase"
Cohesion: 0.50
Nodes (4): RecPhase, Idle, Ready, Recording

### Community 67 - "StepId"
Cohesion: 0.18
Nodes (11): StepId, Allergy, Breath, Call112, Calm, Diabetes, Family, Heart (+3 more)

### Community 70 - "WakeReason"
Cohesion: 0.22
Nodes (9): WakeReason, Checkin, Evening, Fall, FamilyMessage, Heat, Medicine, Morning (+1 more)

### Community 76 - "CoroutineWorker"
Cohesion: 0.32
Nodes (3): ActivityWorker, ReminderWorker, SyncWorker

### Community 81 - "Plans.kt"
Cohesion: 0.29
Nodes (5): Answer, Fine, Help, Unknown, EmergencyPlan

### Community 83 - "TalkMode"
Cohesion: 0.40
Nodes (5): TalkMode, Idle, Listening, Speaking, Thinking

## Knowledge Gaps
- **94 isolated node(s):** `Notifications`, `Mic`, `Camera`, `Usage`, `FullScreen` (+89 more)
  These have ≤1 connection - possible missing edges. (Counts symbols only; 307 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **52 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `jo()` connect `Repo` to `MainViewModel`, `TalkViewModel`, `Wake`, `MainViewModel`, `Onboarding.kt`, `MainViewModel.kt`, `FallDetector.kt`, `SafeArea.kt`, `Monitor.kt`, `.check`, `TalkViewModel.kt`, `VoiceAgent.kt`, `Host`, `Models.kt`, `Actions`, `Screens.kt`, `.conversation`, `.getProfile`, `Wake.kt`, `CoroutineWorker`, `.audit`, `.openEmergencyScreen`?**
  _High betweenness centrality (0.077) - this node is a cross-community bridge._
- **Are the 23 inferred relationships involving `jo()` (e.g. with `.addMemory()` and `.deleteMedicine()`) actually correct?**
  _`jo()` has 23 INFERRED edges - model-reasoned connections that need verification._
- **What connects `Notifications`, `Mic`, `Camera` to the rest of the system?**
  _94 weakly-connected nodes found - possible missing edges._
- **Should `MainViewModel` be split into smaller, more focused modules?**
  _Cohesion score 0.10957910014513789 - nodes in this community are weakly interconnected._
- **Why does `MainViewModel` connect `MainViewModel` to `MainViewModel`, `Repo`, `MainActivity.kt`, `Components.kt`, `App.kt`, `Onboarding.kt`, `MedicinesScreen.kt`, `DoseLog`, `Screens.kt`, `HomeScreen`, `MainViewModel.kt`?**
  _High betweenness centrality (0.066) - this node is a cross-community bridge._
- **Are the 11 inferred relationships involving `SettingsScreen()` (e.g. with `AppNav()` and `Body()`) actually correct?**
  _`SettingsScreen()` has 11 INFERRED edges - model-reasoned connections that need verification._
- **Should `Repo` be split into smaller, more focused modules?**
  _Cohesion score 0.05206611570247934 - nodes in this community are weakly interconnected._