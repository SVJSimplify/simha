# Graph Report - Simha  (2026-11-23)

## Corpus Check
- 90 files · ~57,227 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 74 file(s) not represented in the graph (top: .xml 70, (none) 1, .ttf 1)
- WARNING: 74 of 172 files seen (43%) have no supported extractor and are missing from this graph entirely. It may look complete while representing only part of the codebase.

## Summary
- 1794 nodes · 5253 edges · 90 communities (38 shown, 52 thin omitted)
- Extraction: 94% EXTRACTED · 6% INFERRED · 0% AMBIGUOUS · INFERRED: 316 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `1d678458`
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
- Components.kt
- .decide
- with
- UpdateWorker.kt
- Updater.kt
- Wake
- GuardianRepo
- SarvamHealthTest.kt
- Theme.kt
- Onboarding.kt
- Home.kt
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
- Perm
- Voice
- SafeArea.kt
- MainActivity.kt
- .check
- FakeHost
- SimhaApp.kt
- Repo.kt
- MsgRecorder
- ActivityService.kt
- Voice.kt
- .finish
- TalkViewModel.kt
- jo
- LlmStore
- Host
- .listenSarvam
- HealthMemory
- ContactInfo
- Screens.kt
- Open
- ConversationPanel.kt
- WakeReq
- .envelope
- RecPhase
- ContactEntity
- AlarmReceiver.kt
- SimhaOrb
- WakeReason
- Actions.kt
- FillerGate
- Oem

## God Nodes (most connected - your core abstractions)
1. `MainViewModel` - 72 edges
2. `jo()` - 58 edges
3. `Repo` - 50 edges
4. `FakeHost` - 45 edges
5. `Wake` - 41 edges
6. `Body()` - 38 edges
7. `ToolHost` - 35 edges
8. `TalkViewModel` - 34 edges
9. `Snap` - 32 edges
10. `Voice` - 32 edges

## Surprising Connections (you probably didn't know these)
- `Voice` --calls--> `SarvamClient`  [INFERRED]
  app/src/main/java/com/simha/app/voice/Voice.kt → app/src/main/java/com/simha/app/voice/SarvamClient.kt
- `SimhaOrb()` --calls--> `Stroke`  [INFERRED]
  app/src/main/java/com/simha/app/ui/home/SimhaOrb.kt → app/src/main/java/com/simha/app/logic/FirstAid.kt
- `MedicinesScreen()` --calls--> `todayDoses()`  [INFERRED]
  app/src/main/java/com/simha/app/ui/meds/MedicinesScreen.kt → app/src/main/java/com/simha/app/ui/meds/MedSchedule.kt
- `SimhaRoot()` --calls--> `ProvideLayout()`  [INFERRED]
  app/src/main/java/com/simha/app/ui/App.kt → app/src/main/java/com/simha/app/ui/Components.kt
- `GuardianContext` --references--> `WakeReason`  [EXTRACTED]
  app/src/main/java/com/simha/app/llm/Guardian.kt → app/src/main/java/com/simha/app/wake/Wake.kt

## Import Cycles
- None detected.

## Communities (90 total, 52 thin omitted)

### Community 0 - "MainViewModel"
Cohesion: 0.05
Nodes (79): Snap, MainViewModel, TodayItem, TodayState, AppNav(), BottomBar(), tabSwitch(), AuditScreen() (+71 more)

### Community 2 - "Db.kt"
Cohesion: 0.09
Nodes (9): DiaryEntity, DoseLogEntity, GuardianDao, MedicineEntity, MemoryEntity, OutboxEntity, ProfileEntity, SimhaDao (+1 more)

### Community 3 - "TalkViewModel"
Cohesion: 0.16
Nodes (5): ConversationPanel(), TalkScreen(), ChatLine, TalkViewModel, UiMsg

### Community 5 - "MedicalProfile"
Cohesion: 0.16
Nodes (6): Guardian, GuardianContext, Known, MedLine, MedicalProfile, OpeningFillerTest

### Community 6 - "PlansTest"
Cohesion: 0.07
Nodes (25): Ago, Days, Hours, Minutes, Never, Now, Answer, Fine (+17 more)

### Community 7 - "Components.kt"
Cohesion: 0.05
Nodes (3): Layout, ProvideLayout(), rememberLayout()

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
Cohesion: 0.07
Nodes (8): Monitor, UsageReader, Wake, WakeAudio, WakeActivity, RoutineStore, WakeRequest, WakeStore

### Community 17 - "SarvamHealthTest.kt"
Cohesion: 0.11
Nodes (6): Endpointer, EpState, Done, Timeout, Waiting, SarvamHealthTest

### Community 18 - "Theme.kt"
Cohesion: 0.09
Nodes (4): SimhaColors, nunito(), scheme(), SimhaTheme()

### Community 19 - "Onboarding.kt"
Cohesion: 0.06
Nodes (3): openMaps(), PermText, permText()

### Community 21 - "SarvamConfig"
Cohesion: 0.21
Nodes (4): SarvamConfig, Sarvam, SarvamException, SarvamClient

### Community 22 - "DoseLog"
Cohesion: 0.06
Nodes (37): AuditRow, CallRow, CaregiverRow, Diary, DiaryRow, DoseLog, DoseRow, EventRow (+29 more)

### Community 23 - "LlmToolsTest.kt"
Cohesion: 0.13
Nodes (6): Agent, ChatApi, Msg, ChatApi, ChatApi, ChatApi

### Community 25 - ".stream"
Cohesion: 0.13
Nodes (10): LlmClient, LlmConfig, History, LlmException, LlmParse, ParsedReply, SentenceSplitter, StreamAcc (+2 more)

### Community 26 - "SimhaApp.kt"
Cohesion: 0.27
Nodes (4): SimhaDb, ServiceLocator, SimhaApp, Recorder

### Community 29 - ".execute"
Cohesion: 0.04
Nodes (3): SimhaTools, ToolHost, HealthHost

### Community 30 - "MainViewModel.kt"
Cohesion: 0.05
Nodes (8): K, toUi(), UiKeys, UiSettings, WakeWordEngine, RecognitionListener, WakeWordMatcher, WakeWordPrefs

### Community 31 - "Situation"
Cohesion: 0.07
Nodes (23): DiaryMerge, Scam, Sig, ScamReport, FirstAid, Guide, Situation, Allergy (+15 more)

### Community 32 - "Weather"
Cohesion: 0.11
Nodes (4): Weather, WeatherReport, WakeMath, WakeMathTest

### Community 33 - "FallDetector.kt"
Cohesion: 0.12
Nodes (7): FallDetector, FallMachine, Phase, Free, Idle, Impact, FallMachineTest

### Community 34 - ".learn"
Cohesion: 0.20
Nodes (3): NightState, Routine, RoutineTest

### Community 35 - "Perm"
Cohesion: 0.19
Nodes (11): Perm, Battery, CallPhone, Camera, FullScreen, Mic, Notifications, Overlay (+3 more)

### Community 46 - "Voice.kt"
Cohesion: 0.17
Nodes (6): RecognitionListener, VoiceFail, VoiceKind, Failed, NoSpeech, Unavailable

### Community 48 - "TalkViewModel.kt"
Cohesion: 0.14
Nodes (8): TalkEvent, Done, OpenEmergency, TalkMode, Idle, Listening, Speaking, Thinking

### Community 49 - "jo"
Cohesion: 0.25
Nodes (3): jo(), Prefs, Repo

### Community 50 - "LlmStore"
Cohesion: 0.21
Nodes (5): LlmStore, AgentCall, AgentNotConfiguredException, SarvamVoiceAgent, VoiceAgent

### Community 56 - "Screens.kt"
Cohesion: 0.18
Nodes (3): OBK, openMaps(), PickPhone

### Community 57 - "Open"
Cohesion: 0.15
Nodes (13): Open, AskMedicines, Checkin, Evening, Fall, Family, FamilyGeneric, Heat (+5 more)

### Community 58 - "ConversationPanel.kt"
Cohesion: 0.09
Nodes (11): Kind, Emergency, Primary, Secondary, Success, FadeWords(), VoiceIssue, LangMissing (+3 more)

### Community 59 - "WakeReq"
Cohesion: 0.21
Nodes (9): WakeReady, WakeReq, Autostart, Battery, Exact, FullScreen, Mic, Notifications (+1 more)

### Community 61 - "RecPhase"
Cohesion: 0.50
Nodes (4): RecPhase, Idle, Ready, Recording

### Community 62 - "ContactEntity"
Cohesion: 0.21
Nodes (6): ContactEntity, CallResult, Called, Dialer, Failed, Escalation

### Community 68 - "SimhaOrb"
Cohesion: 0.14
Nodes (8): Stroke, OrbShader, OrbState, Idle, Listening, Speaking, Thinking, SimhaOrb()

### Community 70 - "WakeReason"
Cohesion: 0.22
Nodes (9): WakeReason, Checkin, Evening, Fall, FamilyMessage, Heat, Medicine, Morning (+1 more)

## Knowledge Gaps
- **106 isolated node(s):** `Checkin`, `Medicine`, `Morning`, `Evening`, `Fall` (+101 more)
  These have ≤1 connection - possible missing edges. (Counts symbols only; 335 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **52 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `jo()` connect `jo` to `MainViewModel`, `Db.kt`, `TalkViewModel`, `Actions`, `.decide`, `UpdateWorker.kt`, `Wake`, `GuardianRepo`, `Onboarding.kt`, `DoseLog`, `.execute`, `MainViewModel.kt`, `FallDetector.kt`, `SafeArea.kt`, `.check`, `TalkViewModel.kt`, `Host`, `Screens.kt`, `ContactEntity`, `Wake.kt`, `.audit`?**
  _High betweenness centrality (0.093) - this node is a cross-community bridge._
- **Are the 23 inferred relationships involving `jo()` (e.g. with `.addMemory()` and `.deleteMedicine()`) actually correct?**
  _`jo()` has 23 INFERRED edges - model-reasoned connections that need verification._
- **What connects `Checkin`, `Medicine`, `Morning` to the rest of the system?**
  _106 weakly-connected nodes found - possible missing edges._
- **Should `MainViewModel` be split into smaller, more focused modules?**
  _Cohesion score 0.05056595789696393 - nodes in this community are weakly interconnected._
- **Why does `Repo` connect `jo` to `Db.kt`, `Voice`, `SafeArea.kt`, `.decide`, `Repo.kt`, `GuardianRepo`, `Settings.kt`, `TalkViewModel.kt`, `Onboarding.kt`, `Home.kt`, `Screens.kt`, `SimhaApp.kt`, `ContactEntity`?**
  _High betweenness centrality (0.074) - this node is a cross-community bridge._
- **Should `Db.kt` be split into smaller, more focused modules?**
  _Cohesion score 0.09042553191489362 - nodes in this community are weakly interconnected._
- **Why does `MainViewModel` connect `MainViewModel` to `Db.kt`, `MainActivity.kt`, `Settings.kt`, `Onboarding.kt`, `Home.kt`, `DoseLog`, `MainViewModel.kt`, `Screens.kt`, `ContactEntity`?**
  _High betweenness centrality (0.044) - this node is a cross-community bridge._