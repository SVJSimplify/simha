# Graph Report - Simha  (2026-11-23)

## Corpus Check
- 85 files · ~51,968 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 57 file(s) not represented in the graph (top: .xml 53, (none) 1, .ttf 1)
- WARNING: 57 of 150 files seen (38%) have no supported extractor and are missing from this graph entirely. It may look complete while representing only part of the codebase.

## Summary
- 1657 nodes · 4753 edges · 81 communities (33 shown, 48 thin omitted)
- Extraction: 95% EXTRACTED · 5% INFERRED · 0% AMBIGUOUS · INFERRED: 242 edges (avg confidence: 0.82)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `00501794`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- MainViewModel
- Wake.kt
- Repo
- TalkViewModel.kt
- Actions
- Voice.kt
- PlansTest
- Components.kt
- .decide
- with
- UpdateWorker.kt
- Wake
- DoseLog
- MainViewModel
- SarvamHealthTest.kt
- Theme.kt
- Onboarding.kt
- Home.kt
- SarvamConfig
- VoiceIssue
- build
- App.kt
- HealthHost
- .execute
- MainViewModel.kt
- Situation
- Weather
- FallDetector.kt
- .learn
- SimhaOrb.kt
- Voice
- .put
- MainActivity.kt
- UpdateState.kt
- FakeHost
- SimhaApp.kt
- MsgRecorder
- .begin
- SafeArea.kt
- RecognitionListener
- .finish
- Escalation.kt
- Updater.kt
- VoiceAgent.kt
- Host
- AlarmReceiver.kt
- UsageReader.kt
- HealthMemory
- .aux
- Uri
- Scam
- UpdateInstallReceiver.kt
- Kind
- RecPhase
- Context
- ActivityService.kt
- .listenSarvam

## God Nodes (most connected - your core abstractions)
1. `MainViewModel` - 65 edges
2. `jo()` - 58 edges
3. `Repo` - 50 edges
4. `FakeHost` - 43 edges
5. `SettingsScreen()` - 43 edges
6. `Wake` - 39 edges
7. `ToolHost` - 34 edges
8. `TalkViewModel` - 33 edges
9. `Body()` - 32 edges
10. `Host` - 29 edges

## Surprising Connections (you probably didn't know these)
- `SimhaOrb()` --calls--> `Stroke`  [INFERRED]
  app/src/main/java/com/simha/app/ui/home/SimhaOrb.kt → app/src/main/java/com/simha/app/logic/FirstAid.kt
- `SimhaRoot()` --calls--> `ProvideLayout()`  [INFERRED]
  app/src/main/java/com/simha/app/ui/App.kt → app/src/main/java/com/simha/app/ui/Components.kt
- `AppNav()` --calls--> `animMs()`  [INFERRED]
  app/src/main/java/com/simha/app/ui/App.kt → app/src/main/java/com/simha/app/ui/Components.kt
- `AppNav()` --calls--> `HomeScreen()`  [INFERRED]
  app/src/main/java/com/simha/app/ui/App.kt → app/src/main/java/com/simha/app/ui/Home.kt
- `AppNav()` --calls--> `SettingsScreen()`  [INFERRED]
  app/src/main/java/com/simha/app/ui/App.kt → app/src/main/java/com/simha/app/ui/Settings.kt

## Import Cycles
- None detected.

## Communities (81 total, 48 thin omitted)

### Community 0 - "MainViewModel"
Cohesion: 0.16
Nodes (30): Snap, AppNav(), AuditScreen(), CaregiverHomeScreen(), CaregiverSetupScreen(), eventLabel(), shortTime(), VoiceMessageCard() (+22 more)

### Community 2 - "Repo"
Cohesion: 0.06
Nodes (17): ContactEntity, DiaryEntity, DoseLogEntity, GuardianDao, MedicineEntity, MemoryEntity, OutboxEntity, ProfileEntity (+9 more)

### Community 3 - "TalkViewModel.kt"
Cohesion: 0.05
Nodes (28): Guardian, GuardianContext, MedLine, MedicalProfile, ConversationPanel(), TalkScreen(), ChatLine, TalkEvent (+20 more)

### Community 4 - "Actions"
Cohesion: 0.12
Nodes (11): Actions, Perm, Battery, CallPhone, Camera, FullScreen, Mic, Notifications (+3 more)

### Community 6 - "PlansTest"
Cohesion: 0.07
Nodes (25): Ago, Days, Hours, Minutes, Never, Now, Answer, Fine (+17 more)

### Community 7 - "Components.kt"
Cohesion: 0.08
Nodes (18): animMs(), BigButton(), ButtonCore(), Chip(), ChipGroup(), EmergencyButton(), Grid(), OptionRow() (+10 more)

### Community 8 - ".decide"
Cohesion: 0.17
Nodes (9): Approvals, Decision, AlertFamily, EscalateCallFamily, None, PromptCheckin, Rules, RulesEngine (+1 more)

### Community 9 - "with"
Cohesion: 0.06
Nodes (33): runs-on, steps, concurrency, cancel-in-progress, group, env, KEYSTORE_BASE64, VERSION_CODE (+25 more)

### Community 10 - "UpdateWorker.kt"
Cohesion: 0.12
Nodes (4): ActivityWorker, ReminderWorker, SyncWorker, UpdateWorker

### Community 13 - "DoseLog"
Cohesion: 0.06
Nodes (37): AuditRow, CallRow, CaregiverRow, Diary, DiaryRow, DoseLog, DoseRow, EventRow (+29 more)

### Community 15 - "MainViewModel"
Cohesion: 0.12
Nodes (4): MainViewModel, hoursText(), minutesText(), SettingsScreen()

### Community 17 - "SarvamHealthTest.kt"
Cohesion: 0.12
Nodes (5): EpState, Done, Timeout, Waiting, Wav

### Community 18 - "Theme.kt"
Cohesion: 0.11
Nodes (4): SimhaColors, nunito(), scheme(), SimhaTheme()

### Community 19 - "Onboarding.kt"
Cohesion: 0.12
Nodes (5): OBK, Layout, openMaps(), PermText, permText()

### Community 20 - "Home.kt"
Cohesion: 0.07
Nodes (12): agoText(), ActionTile(), HomeScreen(), TodayRow(), todayText(), doseStatusText(), MedicineForm(), MedicinesScreen() (+4 more)

### Community 21 - "SarvamConfig"
Cohesion: 0.18
Nodes (4): SarvamConfig, Sarvam, SarvamException, SarvamClient

### Community 22 - "VoiceIssue"
Cohesion: 0.50
Nodes (4): VoiceIssue, LangMissing, None, Unavailable

### Community 26 - "HealthHost"
Cohesion: 0.24
Nodes (3): Endpointer, HealthHost, SarvamHealthTest

### Community 30 - "MainViewModel.kt"
Cohesion: 0.06
Nodes (11): K, Prefs, TodayItem, TodayState, toUi(), UiKeys, UiSettings, WakeWord (+3 more)

### Community 31 - "Situation"
Cohesion: 0.07
Nodes (21): DiaryMerge, FirstAid, Guide, Situation, Allergy, Anaphylaxis, Bleeding, Breathless (+13 more)

### Community 32 - "Weather"
Cohesion: 0.11
Nodes (4): Weather, WeatherReport, WakeMath, WakeMathTest

### Community 33 - "FallDetector.kt"
Cohesion: 0.12
Nodes (7): FallDetector, FallMachine, Phase, Free, Idle, Impact, FallMachineTest

### Community 34 - ".learn"
Cohesion: 0.20
Nodes (3): NightState, Routine, RoutineTest

### Community 35 - "SimhaOrb.kt"
Cohesion: 0.09
Nodes (7): Stroke, OrbState, Idle, Listening, Speaking, Thinking, SimhaOrb()

### Community 40 - "FakeHost"
Cohesion: 0.07
Nodes (18): Agent, ChatApi, LlmClient, LlmConfig, History, LlmException, LlmParse, Msg (+10 more)

### Community 45 - "SafeArea.kt"
Cohesion: 0.10
Nodes (4): Location, SafeArea, LocationListener, SafeMath

### Community 48 - "Escalation.kt"
Cohesion: 0.20
Nodes (5): CallResult, Called, Dialer, Failed, Escalation

### Community 49 - "Updater.kt"
Cohesion: 0.18
Nodes (3): UpdateException, UpdateInfo, Updater

### Community 50 - "VoiceAgent.kt"
Cohesion: 0.17
Nodes (5): LlmStore, AgentCall, AgentNotConfiguredException, SarvamVoiceAgent, VoiceAgent

### Community 58 - "Scam"
Cohesion: 0.50
Nodes (3): Scam, Sig, ScamReport

### Community 60 - "Kind"
Cohesion: 0.40
Nodes (5): Kind, Emergency, Primary, Secondary, Success

### Community 61 - "RecPhase"
Cohesion: 0.50
Nodes (4): RecPhase, Idle, Ready, Recording

### Community 67 - ".listenSarvam"
Cohesion: 0.29
Nodes (5): VoiceFail, VoiceKind, Failed, NoSpeech, Unavailable

## Knowledge Gaps
- **86 isolated node(s):** `Allergy`, `Anaphylaxis`, `Fall`, `ChestPain`, `LowSugar` (+81 more)
  These have ≤1 connection - possible missing edges. (Counts symbols only; 300 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **48 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `jo()` connect `Repo` to `MainViewModel`, `FallDetector.kt`, `Wake.kt`, `TalkViewModel.kt`, `Actions`, `.logSymptom`, `Monitor.kt`, `UpdateWorker.kt`, `Wake`, `DoseLog`, `SafeArea.kt`, `MainViewModel`, `Escalation.kt`, `Onboarding.kt`, `Host`, `.aux`, `.execute`, `MainViewModel.kt`?**
  _High betweenness centrality (0.100) - this node is a cross-community bridge._
- **Are the 23 inferred relationships involving `jo()` (e.g. with `.addMemory()` and `.deleteMedicine()`) actually correct?**
  _`jo()` has 23 INFERRED edges - model-reasoned connections that need verification._
- **What connects `Allergy`, `Anaphylaxis`, `Fall` to the rest of the system?**
  _86 weakly-connected nodes found - possible missing edges._
- **Should `Repo` be split into smaller, more focused modules?**
  _Cohesion score 0.05711068211068211 - nodes in this community are weakly interconnected._
- **Why does `Repo` connect `Repo` to `TalkViewModel.kt`, `Voice.kt`, `SimhaApp.kt`, `Monitor.kt`, `SafeArea.kt`, `Onboarding.kt`, `Home.kt`, `MainViewModel.kt`?**
  _High betweenness centrality (0.085) - this node is a cross-community bridge._
- **Are the 12 inferred relationships involving `SettingsScreen()` (e.g. with `AppNav()` and `.remember()`) actually correct?**
  _`SettingsScreen()` has 12 INFERRED edges - model-reasoned connections that need verification._
- **Should `TalkViewModel.kt` be split into smaller, more focused modules?**
  _Cohesion score 0.051189400782896716 - nodes in this community are weakly interconnected._