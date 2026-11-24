# Graph Report - Simha  (2026-11-24)

## Corpus Check
- 93 files · ~60,720 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 79 file(s) not represented in the graph (top: .xml 75, (none) 1, .ttf 1)
- WARNING: 79 of 180 files seen (44%) have no supported extractor and are missing from this graph entirely. It may look complete while representing only part of the codebase.

## Summary
- 1883 nodes · 5452 edges · 113 communities (46 shown, 67 thin omitted)
- Extraction: 94% EXTRACTED · 6% INFERRED · 0% AMBIGUOUS · INFERRED: 317 edges (avg confidence: 0.82)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `c7e0a3d6`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- MainViewModel
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
- OrbGlView
- ConversationPanel.kt
- Weather
- Query
- Theme.kt
- Home.kt
- MainViewModel.kt
- SarvamConfig
- Sig
- LlmToolsTest.kt
- .stream
- Scam
- .execute
- Situation
- FallDetector.kt
- .learn
- Perm
- Voice
- App.kt
- MainActivity.kt
- .check
- FakeHost
- SimhaApp.kt
- MsgRecorder
- GuardianTest
- Voice.kt
- .finish
- TalkViewModel.kt
- UsageReader.kt
- LlmStore
- UpdateInstallReceiver.kt
- Onboarding.kt
- SarvamHealthTest.kt
- ContactInfo
- Screens.kt
- Open
- Voice.kt
- WakeReq
- .envelope
- SimhaApp.kt
- Escalation.kt
- GuardianRepo
- Weather.kt
- .uid
- .addMemory
- Components.kt
- WakeActivity.kt
- WakeReason
- DoseLog
- FillerGate
- Oem
- Host
- nextOccurrence
- .status
- .listenSarvam
- RecPhase

## God Nodes (most connected - your core abstractions)
1. `MainViewModel` - 72 edges
2. `jo()` - 60 edges
3. `Repo` - 50 edges
4. `FakeHost` - 45 edges
5. `Wake` - 41 edges
6. `Body()` - 38 edges
7. `ToolHost` - 35 edges
8. `TalkViewModel` - 33 edges
9. `GuardianRepo` - 32 edges
10. `Snap` - 32 edges

## Surprising Connections (you probably didn't know these)
- `Voice` --calls--> `SarvamClient`  [INFERRED]
  app/src/main/java/com/simha/app/voice/Voice.kt → app/src/main/java/com/simha/app/voice/SarvamClient.kt
- `MedicinesScreen()` --calls--> `todayDoses()`  [INFERRED]
  app/src/main/java/com/simha/app/ui/meds/MedicinesScreen.kt → app/src/main/java/com/simha/app/ui/meds/MedSchedule.kt
- `SimhaRoot()` --calls--> `ProvideLayout()`  [INFERRED]
  app/src/main/java/com/simha/app/ui/App.kt → app/src/main/java/com/simha/app/ui/Components.kt
- `SimhaOrb()` --calls--> `Stroke`  [INFERRED]
  app/src/main/java/com/simha/app/ui/home/SimhaOrb.kt → app/src/main/java/com/simha/app/logic/FirstAid.kt
- `MainActivity` --references--> `MainViewModel`  [EXTRACTED]
  app/src/main/java/com/simha/app/MainActivity.kt → app/src/main/java/com/simha/app/MainViewModel.kt

## Import Cycles
- None detected.

## Communities (113 total, 67 thin omitted)

### Community 0 - "MainViewModel"
Cohesion: 0.06
Nodes (74): Snap, MainViewModel, TodayItem, TodayState, AppNav(), AuditScreen(), CaregiverHomeScreen(), CaregiverSetupScreen() (+66 more)

### Community 2 - "Db.kt"
Cohesion: 0.12
Nodes (5): ContactEntity, FactEntity, OutboxEntity, ProfileEntity, SimhaDao

### Community 3 - "TalkViewModel"
Cohesion: 0.16
Nodes (5): ConversationPanel(), TalkScreen(), ChatLine, TalkViewModel, UiMsg

### Community 5 - "MedicalProfile"
Cohesion: 0.17
Nodes (6): Guardian, GuardianContext, Known, MedLine, MedicalProfile, OpeningFillerTest

### Community 6 - "PlansTest"
Cohesion: 0.07
Nodes (24): Ago, Days, Hours, Minutes, Never, Now, Answer, Fine (+16 more)

### Community 7 - "jo"
Cohesion: 0.26
Nodes (3): jo(), Prefs, Repo

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
Cohesion: 0.05
Nodes (11): Location, Monitor, SafeArea, LocationListener, SafeMath, Wake, WakeAudio, WakeActivity (+3 more)

### Community 15 - "ConversationPanel.kt"
Cohesion: 0.10
Nodes (6): Kind, Emergency, Primary, Secondary, Success, FadeWords()

### Community 16 - "Weather"
Cohesion: 0.11
Nodes (11): Weather, WeatherReport, Slot, SlotKind, Checkin, Evening, Heat, Medicine (+3 more)

### Community 17 - "Query"
Cohesion: 0.16
Nodes (4): DailySignalEntity, GuardianDao, MedicineEntity, WeeklySignalEntity

### Community 18 - "Theme.kt"
Cohesion: 0.09
Nodes (5): TimeDialog(), SimhaColors, nunito(), scheme(), SimhaTheme()

### Community 19 - "Home.kt"
Cohesion: 0.10
Nodes (3): parseTime(), showTime(), WakeWord

### Community 20 - "MainViewModel.kt"
Cohesion: 0.05
Nodes (9): K, OBK, toUi(), UiKeys, UiSettings, WakeWordEngine, RecognitionListener, WakeWordMatcher (+1 more)

### Community 21 - "SarvamConfig"
Cohesion: 0.18
Nodes (4): SarvamConfig, Sarvam, SarvamException, SarvamClient

### Community 22 - "Sig"
Cohesion: 0.09
Nodes (23): AuditRow, CallRow, CaregiverRow, DailySignalRow, Diary, EventRow, FamilyMessage, RecordingRow (+15 more)

### Community 23 - "LlmToolsTest.kt"
Cohesion: 0.14
Nodes (7): Agent, ChatApi, History, Msg, SentenceSplitter, ChatApi, ChatApi

### Community 25 - ".stream"
Cohesion: 0.21
Nodes (8): LlmClient, LlmConfig, LlmException, LlmParse, ParsedReply, StreamAcc, CallAcc, ToolCall

### Community 26 - "Scam"
Cohesion: 0.50
Nodes (3): Scam, Sig, ScamReport

### Community 29 - ".execute"
Cohesion: 0.04
Nodes (3): SimhaTools, ToolHost, HealthHost

### Community 31 - "Situation"
Cohesion: 0.11
Nodes (18): FirstAid, Guide, Situation, Allergy, Anaphylaxis, Bleeding, Breathless, Burn (+10 more)

### Community 33 - "FallDetector.kt"
Cohesion: 0.07
Nodes (10): ActivityService, FallDetector, FallMachine, Phase, Free, Idle, Impact, AlarmReceiver (+2 more)

### Community 34 - ".learn"
Cohesion: 0.20
Nodes (3): NightState, Routine, RoutineTest

### Community 35 - "Perm"
Cohesion: 0.19
Nodes (11): Perm, Battery, CallPhone, Camera, FullScreen, Mic, Notifications, Overlay (+3 more)

### Community 37 - "App.kt"
Cohesion: 0.09
Nodes (3): BottomBar(), SimhaRoot(), tabSwitch()

### Community 39 - ".check"
Cohesion: 0.17
Nodes (5): UpdateException, UpdateInfo, Updater, UpdateState, UpdateUi

### Community 46 - "Voice.kt"
Cohesion: 0.16
Nodes (6): RecognitionListener, VoiceFail, VoiceKind, Failed, NoSpeech, Unavailable

### Community 48 - "TalkViewModel.kt"
Cohesion: 0.13
Nodes (8): TalkEvent, Done, OpenEmergency, TalkMode, Idle, Listening, Speaking, Thinking

### Community 50 - "LlmStore"
Cohesion: 0.18
Nodes (5): LlmStore, AgentCall, AgentNotConfiguredException, SarvamVoiceAgent, VoiceAgent

### Community 52 - "Onboarding.kt"
Cohesion: 0.13
Nodes (3): openMaps(), PermText, permText()

### Community 54 - "SarvamHealthTest.kt"
Cohesion: 0.09
Nodes (8): HealthMemory, Endpointer, EpState, Done, Timeout, Waiting, Wav, SarvamHealthTest

### Community 56 - "Screens.kt"
Cohesion: 0.20
Nodes (5): Step, openMaps(), PickPhone, rememberPicker(), stepText()

### Community 57 - "Open"
Cohesion: 0.15
Nodes (13): Open, AskMedicines, Checkin, Evening, Fall, Family, FamilyGeneric, Heat (+5 more)

### Community 58 - "Voice.kt"
Cohesion: 0.17
Nodes (4): VoiceIssue, LangMissing, None, Unavailable

### Community 59 - "WakeReq"
Cohesion: 0.21
Nodes (9): WakeReady, WakeReq, Autostart, Battery, Exact, FullScreen, Mic, Notifications (+1 more)

### Community 61 - "SimhaApp.kt"
Cohesion: 0.20
Nodes (5): SimhaDb, Remote, ServiceLocator, SimhaApp, Recorder

### Community 62 - "Escalation.kt"
Cohesion: 0.20
Nodes (5): CallResult, Called, Dialer, Failed, Escalation

### Community 67 - ".addMemory"
Cohesion: 0.25
Nodes (4): Memory, Fact, Facts, FactsResult

### Community 68 - "Components.kt"
Cohesion: 0.05
Nodes (4): Layout, pressScale(), ProvideLayout(), rememberLayout()

### Community 69 - "WakeActivity.kt"
Cohesion: 0.12
Nodes (7): Stroke, OrbState, Idle, Listening, Speaking, Thinking, SimhaOrb()

### Community 70 - "WakeReason"
Cohesion: 0.22
Nodes (9): WakeReason, Checkin, Evening, Fall, FamilyMessage, Heat, Medicine, Morning (+1 more)

### Community 71 - "DoseLog"
Cohesion: 0.34
Nodes (6): DoseLog, Medicine, DueDose, nextDose(), todayDoses(), MedScheduleTest

### Community 86 - "nextOccurrence"
Cohesion: 0.40
Nodes (3): isDuplicateMemory(), nextOccurrence(), ScheduleTest

### Community 97 - "RecPhase"
Cohesion: 0.50
Nodes (4): RecPhase, Idle, Ready, Recording

## Knowledge Gaps
- **106 isolated node(s):** `Called`, `Dialer`, `Failed`, `Checkin`, `Medicine` (+101 more)
  These have ≤1 connection - possible missing edges. (Counts symbols only; 362 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **67 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `Repo` connect `jo` to `Settings.kt`, `Db.kt`, `.uid`, `.decide`, `Repo.kt`, `Wake`, `TalkViewModel.kt`, `Query`, `Home.kt`, `Onboarding.kt`, `Screens.kt`, `Voice.kt`, `SimhaApp.kt`, `GuardianRepo`?**
  _High betweenness centrality (0.083) - this node is a cross-community bridge._
- **Are the 26 inferred relationships involving `jo()` (e.g. with `.addMemory()` and `.deleteMedicine()`) actually correct?**
  _`jo()` has 26 INFERRED edges - model-reasoned connections that need verification._
- **What connects `Called`, `Dialer`, `Failed` to the rest of the system?**
  _106 weakly-connected nodes found - possible missing edges._
- **Should `MainViewModel` be split into smaller, more focused modules?**
  _Cohesion score 0.05711786026746657 - nodes in this community are weakly interconnected._
- **Why does `jo()` connect `jo` to `MainViewModel`, `TalkViewModel`, `Actions`, `MedicalProfile`, `.decide`, `UpdateWorker.kt`, `Wake`, `Query`, `MainViewModel.kt`, `Sig`, `.execute`, `FallDetector.kt`, `TalkViewModel.kt`, `Onboarding.kt`, `Screens.kt`, `Escalation.kt`, `GuardianRepo`, `.uid`, `.addMemory`, `Host`?**
  _High betweenness centrality (0.069) - this node is a cross-community bridge._
- **Should `Db.kt` be split into smaller, more focused modules?**
  _Cohesion score 0.1206896551724138 - nodes in this community are weakly interconnected._
- **Why does `ToolHost` connect `.execute` to `.logDiary`, `Weather.kt`, `MainViewModel`, `FakeHost`, `Host`, `TalkViewModel.kt`, `LlmToolsTest.kt`, `SarvamHealthTest.kt`, `ContactInfo`?**
  _High betweenness centrality (0.047) - this node is a cross-community bridge._