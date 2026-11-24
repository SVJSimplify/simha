# Graph Report - Simha  (2026-11-24)

## Corpus Check
- 90 files · ~58,267 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 74 file(s) not represented in the graph (top: .xml 70, (none) 1, .ttf 1)
- WARNING: 74 of 172 files seen (43%) have no supported extractor and are missing from this graph entirely. It may look complete while representing only part of the codebase.

## Summary
- 1824 nodes · 5319 edges · 96 communities (38 shown, 58 thin omitted)
- Extraction: 94% EXTRACTED · 6% INFERRED · 0% AMBIGUOUS · INFERRED: 314 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `7a0c13ef`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- MainViewModel
- Notifier
- jo
- TalkViewModel
- Actions
- MedicalProfile
- PlansTest
- Components.kt
- .decide
- with
- UpdateWorker.kt
- Wake
- OrbGlView
- Body
- SarvamHealthTest
- Theme.kt
- Onboarding.kt
- RecognitionListener
- SarvamConfig
- DoseLog
- LlmToolsTest.kt
- .stream
- .execute
- .execute
- MainViewModel.kt
- Situation
- MedicalId.kt
- FallDetector.kt
- .learn
- Perm
- Voice
- SafeArea.kt
- MainActivity.kt
- .check
- FakeHost
- SimhaApp.kt
- MsgRecorder
- ActivityService.kt
- Voice.kt
- .finish
- TalkViewModel.kt
- UsageReader.kt
- LlmStore
- UpdateInstallReceiver.kt
- StepName
- SarvamHealthTest.kt
- ContactInfo
- Screens.kt
- Open
- FadeWords
- WakeReq
- .envelope
- Escalation.kt
- SafeArea.kt
- SimhaColors
- SimhaOrb.kt
- WakeReason
- FillerGate
- Oem
- UpdateBanner

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
- `SimhaOrb()` --calls--> `Stroke`  [INFERRED]
  app/src/main/java/com/simha/app/ui/home/SimhaOrb.kt → app/src/main/java/com/simha/app/logic/FirstAid.kt
- `Voice` --calls--> `SarvamClient`  [INFERRED]
  app/src/main/java/com/simha/app/voice/Voice.kt → app/src/main/java/com/simha/app/voice/SarvamClient.kt
- `ConversationPanel()` --calls--> `UiMsg`  [INFERRED]
  app/src/main/java/com/simha/app/ui/talk/ConversationPanel.kt → app/src/main/java/com/simha/app/ui/talk/TalkViewModel.kt
- `AppNav()` --calls--> `animMs()`  [INFERRED]
  app/src/main/java/com/simha/app/ui/App.kt → app/src/main/java/com/simha/app/ui/Components.kt
- `AppNav()` --calls--> `HealthScreen()`  [INFERRED]
  app/src/main/java/com/simha/app/ui/App.kt → app/src/main/java/com/simha/app/ui/Health.kt

## Import Cycles
- None detected.

## Communities (96 total, 58 thin omitted)

### Community 0 - "MainViewModel"
Cohesion: 0.11
Nodes (15): Snap, MainViewModel, TodayItem, TodayState, SectionTitle(), SwitchRow(), AboutPage(), CheckinsPage() (+7 more)

### Community 2 - "jo"
Cohesion: 0.05
Nodes (20): ContactEntity, DiaryEntity, DoseLogEntity, GuardianDao, MedicineEntity, MemoryEntity, OutboxEntity, ProfileEntity (+12 more)

### Community 3 - "TalkViewModel"
Cohesion: 0.16
Nodes (3): ChatLine, TalkViewModel, UiMsg

### Community 5 - "MedicalProfile"
Cohesion: 0.16
Nodes (7): Guardian, GuardianContext, Known, MedLine, MedicalProfile, Langs, OpeningFillerTest

### Community 6 - "PlansTest"
Cohesion: 0.07
Nodes (25): Ago, Days, Hours, Minutes, Never, Now, Answer, Fine (+17 more)

### Community 7 - "Components.kt"
Cohesion: 0.08
Nodes (25): agoText(), AnimatedEntrance(), animMs(), ButtonCore(), Chip(), ChipGroup(), EmergencyButton(), Grid() (+17 more)

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
Nodes (13): Weather, WeatherReport, Monitor, SafeArea, Wake, WakeAudio, WakeActivity, Slot (+5 more)

### Community 15 - "Body"
Cohesion: 0.12
Nodes (31): AppNav(), BottomBar(), AuditScreen(), CaregiverHomeScreen(), CaregiverSetupScreen(), eventLabel(), shortTime(), VoiceMessageCard() (+23 more)

### Community 18 - "Theme.kt"
Cohesion: 0.10
Nodes (3): nunito(), scheme(), SimhaTheme()

### Community 19 - "Onboarding.kt"
Cohesion: 0.05
Nodes (6): tabSwitch(), Layout, fadeUp(), openMaps(), PermText, permText()

### Community 20 - "RecognitionListener"
Cohesion: 0.13
Nodes (3): WakeWordEngine, RecognitionListener, WakeWordPrefs

### Community 21 - "SarvamConfig"
Cohesion: 0.22
Nodes (4): SarvamConfig, Sarvam, SarvamException, SarvamClient

### Community 22 - "DoseLog"
Cohesion: 0.06
Nodes (36): AuditRow, CallRow, CaregiverRow, Diary, DiaryRow, DoseLog, DoseRow, EventRow (+28 more)

### Community 23 - "LlmToolsTest.kt"
Cohesion: 0.14
Nodes (10): Agent, ChatApi, History, Msg, ParsedReply, SentenceSplitter, ToolCall, ChatApi (+2 more)

### Community 25 - ".stream"
Cohesion: 0.21
Nodes (6): LlmClient, LlmConfig, LlmException, LlmParse, StreamAcc, CallAcc

### Community 26 - ".execute"
Cohesion: 0.14
Nodes (4): Scam, Sig, ScamReport, SimhaTools

### Community 29 - ".execute"
Cohesion: 0.04
Nodes (3): ToolHost, Host, HealthHost

### Community 30 - "MainViewModel.kt"
Cohesion: 0.09
Nodes (4): K, toUi(), UiKeys, UiSettings

### Community 31 - "Situation"
Cohesion: 0.07
Nodes (22): DiaryMerge, FirstAid, Guide, Situation, Allergy, Anaphylaxis, Bleeding, Breathless (+14 more)

### Community 32 - "MedicalId.kt"
Cohesion: 0.30
Nodes (6): LabeledField(), roleLabel(), line(), MedicalIdNotif, MedicalIdScreen(), medLine()

### Community 33 - "FallDetector.kt"
Cohesion: 0.11
Nodes (8): FallDetector, FallMachine, Phase, Free, Idle, Impact, SafeMath, FallMachineTest

### Community 34 - ".learn"
Cohesion: 0.20
Nodes (3): NightState, Routine, RoutineTest

### Community 35 - "Perm"
Cohesion: 0.19
Nodes (11): Perm, Battery, CallPhone, Camera, FullScreen, Mic, Notifications, Overlay (+3 more)

### Community 39 - ".check"
Cohesion: 0.29
Nodes (3): UpdateException, UpdateInfo, Updater

### Community 48 - "TalkViewModel.kt"
Cohesion: 0.09
Nodes (16): CallResult, Called, Dialer, Failed, TalkEvent, Done, OpenEmergency, TalkMode (+8 more)

### Community 50 - "LlmStore"
Cohesion: 0.19
Nodes (5): LlmStore, AgentCall, AgentNotConfiguredException, SarvamVoiceAgent, VoiceAgent

### Community 52 - "StepName"
Cohesion: 0.62
Nodes (6): ChoiceCard(), OnboardingScreen(), Progress(), StepName(), StepWho(), Up()

### Community 54 - "SarvamHealthTest.kt"
Cohesion: 0.13
Nodes (6): HealthMemory, EpState, Done, Timeout, Waiting, Wav

### Community 56 - "Screens.kt"
Cohesion: 0.15
Nodes (7): OBK, normalizeContactPhone(), ContactsScreen(), openMaps(), PickPhone, rememberPicker(), stepText()

### Community 57 - "Open"
Cohesion: 0.15
Nodes (13): Open, AskMedicines, Checkin, Evening, Fall, Family, FamilyGeneric, Heat (+5 more)

### Community 58 - "FadeWords"
Cohesion: 0.14
Nodes (9): RecPhase, Idle, Ready, Recording, FadeWords(), VoiceIssue, LangMissing, None (+1 more)

### Community 59 - "WakeReq"
Cohesion: 0.21
Nodes (9): WakeReady, WakeReq, Autostart, Battery, Exact, FullScreen, Mic, Notifications (+1 more)

### Community 62 - "Escalation.kt"
Cohesion: 0.21
Nodes (3): UpdateState, UpdateUi, Escalation

### Community 68 - "SimhaOrb.kt"
Cohesion: 0.06
Nodes (6): OrbState, Idle, Listening, Speaking, Thinking, SimhaOrb()

### Community 70 - "WakeReason"
Cohesion: 0.22
Nodes (9): WakeReason, Checkin, Evening, Fall, FamilyMessage, Heat, Medicine, Morning (+1 more)

## Knowledge Gaps
- **106 isolated node(s):** `Checkin`, `Medicine`, `Morning`, `Evening`, `Fall` (+101 more)
  These have ≤1 connection - possible missing edges. (Counts symbols only; 346 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **58 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `jo()` connect `jo` to `MainViewModel`, `FallDetector.kt`, `SafeArea.kt`, `TalkViewModel`, `.getProfile`, `Actions`, `Wake.kt`, `.decide`, `UpdateWorker.kt`, `Wake`, `Body`, `TalkViewModel.kt`, `Onboarding.kt`, `DoseLog`, `Screens.kt`, `Escalation.kt`, `.execute`, `MainViewModel.kt`?**
  _High betweenness centrality (0.066) - this node is a cross-community bridge._
- **Are the 23 inferred relationships involving `jo()` (e.g. with `.addMemory()` and `.deleteMedicine()`) actually correct?**
  _`jo()` has 23 INFERRED edges - model-reasoned connections that need verification._
- **What connects `Checkin`, `Medicine`, `Morning` to the rest of the system?**
  _106 weakly-connected nodes found - possible missing edges._
- **Should `MainViewModel` be split into smaller, more focused modules?**
  _Cohesion score 0.1147086031452359 - nodes in this community are weakly interconnected._
- **Why does `Repo` connect `jo` to `MainViewModel`, `SafeArea.kt`, `Voice`, `.decide`, `Repo.kt`, `TalkViewModel.kt`, `Onboarding.kt`, `Screens.kt`?**
  _High betweenness centrality (0.052) - this node is a cross-community bridge._
- **Should `jo` be split into smaller, more focused modules?**
  _Cohesion score 0.05403447776329132 - nodes in this community are weakly interconnected._
- **Why does `Wake` connect `Wake` to `MainViewModel`, `FallDetector.kt`, `SafeArea.kt`, `TalkViewModel`, `Voice`, `Wake.kt`, `MedicalProfile`, `.decide`, `TalkViewModel.kt`, `MainViewModel.kt`?**
  _High betweenness centrality (0.048) - this node is a cross-community bridge._