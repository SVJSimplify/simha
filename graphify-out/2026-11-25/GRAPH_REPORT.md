# Graph Report - Simha  (2026-11-24)

## Corpus Check
- 95 files · ~62,320 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 79 file(s) not represented in the graph (top: .xml 75, (none) 1, .ttf 1)
- WARNING: 79 of 182 files seen (43%) have no supported extractor and are missing from this graph entirely. It may look complete while representing only part of the codebase.

## Summary
- 1936 nodes · 5609 edges · 111 communities (46 shown, 65 thin omitted)
- Extraction: 94% EXTRACTED · 6% INFERRED · 0% AMBIGUOUS · INFERRED: 325 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `ca0b64a6`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- Body
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
- GuardianRepo
- Onboarding.kt
- WakeMathTest
- Query
- Theme.kt
- build
- WakeWord.kt
- SarvamConfig
- Sig
- LlmToolsTest.kt
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
- Voice.kt
- .finish
- TalkViewModel.kt
- VoiceIssue
- LlmStore
- UpdateInstallReceiver.kt
- .check
- SarvamHealthTest.kt
- SafeArea.kt
- Open
- WakeReq
- .envelope
- SimhaApp.kt
- UpdateState.kt
- .uid
- .addMemory
- Components.kt
- WakeReason
- DoseLog
- .respond
- Oem
- Wake.kt
- nextOccurrence
- SarvamHealthTest
- Caregiver.kt
- HealthMemory
- LocaleCtx.kt
- SlotKind
- Kind

## God Nodes (most connected - your core abstractions)
1. `MainViewModel` - 72 edges
2. `jo()` - 60 edges
3. `FakeHost` - 51 edges
4. `Repo` - 50 edges
5. `Wake` - 41 edges
6. `Body()` - 38 edges
7. `ToolHost` - 37 edges
8. `LlmToolsTest` - 37 edges
9. `TalkViewModel` - 33 edges
10. `GuardianRepo` - 32 edges

## Surprising Connections (you probably didn't know these)
- `SimhaRoot()` --calls--> `ProvideLayout()`  [INFERRED]
  app/src/main/java/com/simha/app/ui/App.kt → app/src/main/java/com/simha/app/ui/Components.kt
- `AppNav()` --calls--> `HealthScreen()`  [INFERRED]
  app/src/main/java/com/simha/app/ui/App.kt → app/src/main/java/com/simha/app/ui/Health.kt
- `AppNav()` --calls--> `HomeScreen()`  [INFERRED]
  app/src/main/java/com/simha/app/ui/App.kt → app/src/main/java/com/simha/app/ui/Home.kt
- `AppNav()` --calls--> `ContactsScreen()`  [INFERRED]
  app/src/main/java/com/simha/app/ui/App.kt → app/src/main/java/com/simha/app/ui/Screens.kt
- `AppNav()` --calls--> `EmergencyScreen()`  [INFERRED]
  app/src/main/java/com/simha/app/ui/App.kt → app/src/main/java/com/simha/app/ui/Screens.kt

## Import Cycles
- None detected.

## Communities (111 total, 65 thin omitted)

### Community 0 - "Body"
Cohesion: 0.12
Nodes (42): BigButton(), Body(), ButtonCore(), Caption(), Card(), Chip(), ChipGroup(), EmergencyButton() (+34 more)

### Community 2 - "Db.kt"
Cohesion: 0.14
Nodes (7): ContactEntity, FactEntity, MedicineEntity, OutboxEntity, ProfileEntity, SimhaDb, ServiceLocator

### Community 3 - "TalkViewModel"
Cohesion: 0.20
Nodes (4): ConversationPanel(), TalkScreen(), TalkViewModel, UiMsg

### Community 5 - "MedicalProfile"
Cohesion: 0.18
Nodes (7): ContactLine, Guardian, GuardianContext, Known, MedLine, MedicalProfile, OpeningFillerTest

### Community 6 - "PlansTest"
Cohesion: 0.07
Nodes (25): Ago, Days, Hours, Minutes, Never, Now, Answer, Fine (+17 more)

### Community 7 - "jo"
Cohesion: 0.26
Nodes (3): jo(), Prefs, Repo

### Community 8 - ".decide"
Cohesion: 0.17
Nodes (9): Approvals, Decision, AlertFamily, EscalateCallFamily, None, PromptCheckin, Rules, RulesEngine (+1 more)

### Community 9 - "with"
Cohesion: 0.06
Nodes (33): runs-on, steps, concurrency, cancel-in-progress, group, env, KEYSTORE_BASE64, VERSION_CODE (+25 more)

### Community 10 - "UpdateWorker.kt"
Cohesion: 0.06
Nodes (6): Notifier, ActivityWorker, ReminderWorker, SyncWorker, Work, UpdateWorker

### Community 12 - "Wake"
Cohesion: 0.06
Nodes (10): Monitor, UsageReader, AlarmReceiver, BootReceiver, Wake, WakeAudio, WakeActivity, RoutineStore (+2 more)

### Community 15 - "Onboarding.kt"
Cohesion: 0.04
Nodes (10): OBK, tabSwitch(), agoText(), Layout, ActionTile(), HomeScreen(), TodayRow(), todayText() (+2 more)

### Community 16 - "WakeMathTest"
Cohesion: 0.14
Nodes (5): Weather, WeatherReport, Slot, WakeMath, WakeMathTest

### Community 17 - "Query"
Cohesion: 0.16
Nodes (4): DailySignalEntity, GuardianDao, SimhaDao, WeeklySignalEntity

### Community 18 - "Theme.kt"
Cohesion: 0.05
Nodes (5): MainActivity, SimhaColors, nunito(), scheme(), SimhaTheme()

### Community 20 - "WakeWord.kt"
Cohesion: 0.05
Nodes (7): Gl, OrbGlView, WakeWord, WakeWordEngine, RecognitionListener, WakeWordMatcher, WakeWordPrefs

### Community 21 - "SarvamConfig"
Cohesion: 0.20
Nodes (4): SarvamConfig, Sarvam, SarvamException, SarvamClient

### Community 22 - "Sig"
Cohesion: 0.09
Nodes (23): AuditRow, CallRow, CaregiverRow, DailySignalRow, Diary, EventRow, FamilyMessage, RecordingRow (+15 more)

### Community 23 - "LlmToolsTest.kt"
Cohesion: 0.09
Nodes (14): Agent, ChatApi, History, LlmException, Msg, ParsedReply, SentenceSplitter, StreamAcc (+6 more)

### Community 25 - ".stream"
Cohesion: 0.31
Nodes (3): LlmClient, LlmConfig, LlmParse

### Community 29 - ".execute"
Cohesion: 0.04
Nodes (4): ContactInfo, ToolHost, Host, HealthHost

### Community 31 - "Situation"
Cohesion: 0.06
Nodes (23): DiaryMerge, Scam, Sig, ScamReport, FirstAid, Guide, Situation, Allergy (+15 more)

### Community 32 - "MainViewModel"
Cohesion: 0.10
Nodes (12): MainViewModel, TodayItem, TodayState, SectionTitle(), showTime(), AboutPage(), CheckinsPage(), hoursText() (+4 more)

### Community 33 - "ActivityService.kt"
Cohesion: 0.08
Nodes (8): ActivityService, FallDetector, FallMachine, Phase, Free, Idle, Impact, FallMachineTest

### Community 34 - ".learn"
Cohesion: 0.20
Nodes (3): NightState, Routine, RoutineTest

### Community 35 - "Perm"
Cohesion: 0.16
Nodes (13): Perm, Battery, CallPhone, Camera, FullScreen, Mic, Notifications, Overlay (+5 more)

### Community 36 - "Voice.kt"
Cohesion: 0.09
Nodes (4): Langs, Recorder, SpeakItem, Voice

### Community 37 - ".wrapped"
Cohesion: 0.24
Nodes (3): LocaleCtx, BottomBar(), SimhaRoot()

### Community 39 - "Updater.kt"
Cohesion: 0.22
Nodes (3): UpdateException, UpdateInfo, Updater

### Community 41 - "Escalation.kt"
Cohesion: 0.22
Nodes (5): CallResult, Called, Dialer, Failed, Escalation

### Community 46 - "Voice.kt"
Cohesion: 0.17
Nodes (6): RecognitionListener, VoiceFail, VoiceKind, Failed, NoSpeech, Unavailable

### Community 48 - "TalkViewModel.kt"
Cohesion: 0.13
Nodes (8): TalkEvent, Done, OpenEmergency, TalkMode, Idle, Listening, Speaking, Thinking

### Community 49 - "VoiceIssue"
Cohesion: 0.50
Nodes (4): VoiceIssue, LangMissing, None, Unavailable

### Community 50 - "LlmStore"
Cohesion: 0.17
Nodes (5): LlmStore, AgentCall, AgentNotConfiguredException, SarvamVoiceAgent, VoiceAgent

### Community 52 - ".check"
Cohesion: 0.24
Nodes (3): Location, SafeArea, SafeMath

### Community 54 - "SarvamHealthTest.kt"
Cohesion: 0.12
Nodes (5): EpState, Done, Timeout, Waiting, Wav

### Community 57 - "Open"
Cohesion: 0.15
Nodes (13): Open, AskMedicines, Checkin, Evening, Fall, Family, FamilyGeneric, Heat (+5 more)

### Community 59 - "WakeReq"
Cohesion: 0.16
Nodes (9): WakeReady, WakeReq, Autostart, Battery, Exact, FullScreen, Mic, Notifications (+1 more)

### Community 67 - ".addMemory"
Cohesion: 0.23
Nodes (4): Memory, Fact, Facts, FactsResult

### Community 68 - "Components.kt"
Cohesion: 0.04
Nodes (9): Stroke, ProvideLayout(), rememberLayout(), OrbState, Idle, Listening, Speaking, Thinking (+1 more)

### Community 70 - "WakeReason"
Cohesion: 0.22
Nodes (9): WakeReason, Checkin, Evening, Fall, FamilyMessage, Heat, Medicine, Morning (+1 more)

### Community 71 - "DoseLog"
Cohesion: 0.23
Nodes (7): DoseLog, Medicine, DoseSlots, DueDose, nextDose(), todayDoses(), MedScheduleTest

### Community 77 - "Wake.kt"
Cohesion: 0.11
Nodes (5): forLang(), K, toUi(), UiKeys, UiSettings

### Community 86 - "nextOccurrence"
Cohesion: 0.32
Nodes (4): isDuplicateMemory(), nextOccurrence(), parseTime(), ScheduleTest

### Community 97 - "Caregiver.kt"
Cohesion: 0.14
Nodes (23): Snap, AppNav(), AuditScreen(), CaregiverHomeScreen(), CaregiverSetupScreen(), eventLabel(), RecPhase, Idle (+15 more)

### Community 119 - "SlotKind"
Cohesion: 0.33
Nodes (6): SlotKind, Checkin, Evening, Heat, Medicine, Morning

### Community 120 - "Kind"
Cohesion: 0.40
Nodes (5): Kind, Emergency, Primary, Secondary, Success

## Knowledge Gaps
- **106 isolated node(s):** `Checkin`, `Medicine`, `Morning`, `Evening`, `Fall` (+101 more)
  These have ≤1 connection - possible missing edges. (Counts symbols only; 363 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **65 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `jo()` connect `jo` to `Body`, `.getProfile`, `Db.kt`, `TalkViewModel`, `UpdateWorker.kt`, `Wake`, `GuardianRepo`, `Onboarding.kt`, `Query`, `Sig`, `.execute`, `MainViewModel`, `ActivityService.kt`, `Escalation.kt`, `TalkViewModel.kt`, `.check`, `SafeArea.kt`, `.uid`, `.addMemory`, `.respond`, `Wake.kt`?**
  _High betweenness centrality (0.075) - this node is a cross-community bridge._
- **Are the 26 inferred relationships involving `jo()` (e.g. with `.addMemory()` and `.deleteMedicine()`) actually correct?**
  _`jo()` has 26 INFERRED edges - model-reasoned connections that need verification._
- **What connects `Checkin`, `Medicine`, `Morning` to the rest of the system?**
  _106 weakly-connected nodes found - possible missing edges._
- **Should `Body` be split into smaller, more focused modules?**
  _Cohesion score 0.11948051948051948 - nodes in this community are weakly interconnected._
- **Why does `Repo` connect `jo` to `MainViewModel`, `Db.kt`, `.uid`, `Voice.kt`, `Repo.kt`, `GuardianRepo`, `Wake.kt`, `Onboarding.kt`, `TalkViewModel.kt`, `Query`, `SafeArea.kt`, `SimhaApp.kt`?**
  _High betweenness centrality (0.050) - this node is a cross-community bridge._
- **Should `.getProfile` be split into smaller, more focused modules?**
  _Cohesion score 0.14166666666666666 - nodes in this community are weakly interconnected._
- **Why does `MainViewModel` connect `MainViewModel` to `Body`, `Caregiver.kt`, `Db.kt`, `.wrapped`, `DoseLog`, `Wake.kt`, `Onboarding.kt`, `Theme.kt`?**
  _High betweenness centrality (0.049) - this node is a cross-community bridge._