# MyCooking
CookingAPP
Abstract  (在android 应用中性能缺陷的静态检测)
For static analysis researchers, Android software presents a wide variety of interesting challenges. The target of our work is static detection of energy-drain defects in Android applications. The management of energy-intensive resources (e.g., GPS) creates various opportunities for software defects.

  Our goal is to detect statically “missing deactivation” energy drain defects in the user interface of the application. First, we define precisely two patterns of run-time energy-drain behaviors, based on modeling of Android GUI control-flow paths and energy-related listener leaks along such paths. Next, we define a static detection algorithm targeting these patterns. The analysis considers valid interprocedural control-flow paths in a callback method and its transitive
callees, in order to detect operations that add or remove listeners. Sequences of callbacks are then analyzed for possible listener leaks. Our evaluation considers the detection of GUI-related energy-drain defects reported in prior work, as well as new defects not discovered by prior approaches. In summary, the detection is
very effective and precise, suggesting that the proposed analysis is suitable for practical use in static checking tools for Android.

 Categories and Subject Descriptors   F.3.2 [Logics and Meaning of Programs]: Semantics of Programming Languages—Program analysis
General Terms  Algorithms, experimentation, measurement
Keywords  Android, GUI analysis, static analysis, energy

1. Introduction
The computing field has changed significantly in the last few years due to the exponential growth in the number of mobile devices such as smartphones and tablets. In this space, Android is the dominant platform [12]. For static analysis researchers, Android software presents a wide variety of interesting challenges, both in terms of foundational control-flow and data-flow analysis techniques and
in terms of specific analyses targeting software correctness, robustness, performance, and security.
The target of our work is static detection of energy-drain defects in Android applications. For mobile devices, the management of energy-intensive resources （e.g GPS) burdens the developer with “power-encumbered programming” [36] and creates various opportunities for software defects. Static detection of such defects is of significant value. Common battery-drain defects—“no-sleep” [36]
and “missing deactivation” [4, 27]—are due to executions along which an energy-draining resource is activated but not properly deactivated. Such dynamic behaviors can be naturally stated as properties of control-flow paths, and thus present desirable targets for static control-flow and data-flow analyses. 

  State of the art  Despite the clear importance of energy-drain defects, there is limited understanding of how to detect such defects with the help of program analysis. Early work on this topic [36] defines a data-flow analysis to identify relevant API calls that turn on some energy-draining resource, and to search for no-sleep code paths along which corresponding turn-off/release calls are missing.
For this approach the control-flow analysis of possible execution paths is of critical importance. However, due to the GUI-based and event-driven nature of Android applications and the complex interactions between application code and framework code through sequences of callbacks, static control-flow modeling is a very challenging problem. This prior work employs an ad hoc control-flow analysis that exhibits significant lack of generality and precision, and involves manual effort by the user. An alternative is to use dynamic analyses to detect certain categories of energy-drain defects [4, 27]. Such defects also correspond to execution paths in
which a sensor (e.g., the GPS) is not put to sleep appropriately, often because of mismanagement of complex callbacks from the Android framework to the application code. However, such run-time detection critically depends on the ability to trigger the problematic behavior. This requires comprehensive GUI run-time exploration, which is a very challenging problem for an automated analysis [8].
It is highly desirable to develop static analyses that employ general control-flow modeling of all possible run-time behaviors, together with detection of the energy-drain patterns along such behaviors.

  Our proposal  We aim to develop a general static analysis approach for detecting certain common categories of energy-drain defects. Specifically, we aim to detect “missing deactivation” behaviors in the user interface thread of the application. This is the main thread of the application and the majority of application logic is executed in it. While such problematic behaviors may also occur in threads running concurrently with the UI thread, the current state of the art for control-flow analysis of multithreaded Android control flow is still very underdeveloped and there is no conceptual clarity on how Android-specific asynchronous constructs (e.g., asynchronous tasks and long-running services) should be modeled statically. Thus, in our current work we focus only on the behavior of the UI thread and the callbacks executed in that thread. Due to recent advances in static control-flow analysis of Android UI behavior [48], the development of static detectors for such energydrain defects is feasible for the first time. Future work on more general static control-flow analysis for multithreaded Android executions will also enable generalizations of our current techniques to additional categories of energy-drain defects.

  The proposed approach is based on three key contributions. First, we define precisely two patterns of run-time energy-drain behaviors �Section 2). The definition is based on formal definitions of relevant aspects of Android GUI run-time control flow, including modeling of GUI events, event handlers, transitions between windows, and the associated sequences of callbacks. This modeling allows us to define the notion of a leaking control-flow path and two defect patterns based on it. These patterns are related to the �mis)use of Android location awareness capabilities �e.g., GPS location information). Location awareness is a major contributor to energy drain [16] and in prior work on dynamic defect detection [27] has been identified as the predominant cause of energy-related defects in UI behavior. Our definition of defect patterns is motivated by case studies from this prior work and by our own analysis of these case studies. However, our careful formulation of these patterns is new and provides a valuable contribution to the state of the art. Furthermore, our control-flow modeling is significantly more general than any prior technique.
  
  The second contribution of our approach is a static defect detection algorithm �Section 3). As a starting point, we use the window transition graph �WTG), a static GUI control-flow model we proposed in prior work [48]. Based on this model, the analysis considers valid interprocedural control-flow paths in each callback
  
  method and its transitive callees, in order to detect operations that add or remove location listeners. Sequences of window transitions and their callbacks are then analyzed for possible listener leaking behaviors based on the two patterns mentioned earlier.
The final contribution of our work is a study of the effectiveness of the proposed static detection �Section 4). We aim to determine how well the analysis discovers GUI-related energy-drain defects reported in prior work, as well as new defects not discovered by prior approaches. Our evaluation on 15 Android applications indicates that the static detection is very effective and is superior
to dynamic detection. Furthermore, all but one of the reported problems are real defects. The evaluation also shows that the cost of the analysis is low. This high precision and low cost suggest that the proposed approach is suitable for practical use in static checking tools for Android.
