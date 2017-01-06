第六页

Follow-up work [46–48] defines the window transition graph
�WTG), a static model G = �Win�Trans� �� δ� σ) with nodes
w ∈ Win and edges t = [w�w�] ∈ Trans. Each transition t
is annotated with trigger event ��t), callback sequence σ�t), and
window stack changes δ�t). Implementations of these analyses are
available in our GATOR [41] analysis toolkit for Android, which
itself is built using the Soot framework [43] and its Jimple internal
representation �constructed either from Java bytecode or from
Dalvik bytecode via Dexpler [5]). The energy defect analysis was
developed in this infrastructure.
The WTGs for the running examples are shown in Figure 4.
These graphs are small because the examples are simplified on
purpose, but in actual applications WTGs have hundreds of edges.
Given the WTG, our detection analysis proceeds in three phases.
3.1 Phase 1: AddListener
and RemoveListener
Operations
Consider the set �[c�o] | t ∈ G∧[c�o] ∈ σ�t)}. For each invocation
of a callback c on object o, we compute a set A�c� o) of pairs [a�l]
of an add-listener API invocation statement a and a listener object
l. We also compute a similar set R�c� o) of pairs [r�l]. These sets
are determined in four steps, as described below.
Step 1 An interprocedural control-flow graph �ICFG) [42] is constructed
for c and its transitive callees in the application code. Then,
a constant propagation analysis is performed to identify and remove
infeasible ICFG edges, based on the knowledge that the calling
context of c is o. This analysis is defined in prior work [47],
where it was shown to produce more precise control-flow models.
For the example in Figure 3, this analysis will determine that
when onOptionsItemSelected is invoked on the item with id
ADD INCIDENT, only one branch of the switch statement is feasible
and the rest of the branches can be ignored. In addition, we
remove ICFG edges related to null pointer checks and throwing of
unchecked exceptions, since in our experience they represent unusual
control flow that does not contribute to defect detection.
Step 2 An ICFG traversal is performed starting from the entry
node of c. This traversal follows interprocedurally-valid paths. During
the traversal, whenever an add-listener API call site a is encountered,
the points-to set of the listener parameter is used to construct
and remember pairs [a�l]. �Points-to sets are derived as described
elsewhere [40].) Similarly, we also record all reached pairs [r�l]
where r is a remove-listener API invocation statement. For the example
in Figure 2, analysis of onClick and its callees will identify
[requestLocationUpdates�EventManager] where the second
element of the pair denotes the listener created at line 12. For
the example in Figure 3, analysis of onCreate will identify a similar
pair with the activity being the listener. In addition, analysis of
onDestroy will detect [removeUpdates�CheckinActivity].
Step 3 For each [a�l] encountered in Step 2, we need to determine
whether the ICFG contains a path from statement a to the exit
of c along which there does not exist a matching remove-listener
operation [r�l]. If the ICFG contains at least one [r�l] for the same
listener l �as determined by Step 2), we perform an additional
traversal on the reverse ICFG, starting from the exit node of c.
This traversal considers only valid paths with proper matching of
call sites and return sites. During the traversal, whenever a removelistener
API call site r with listener l is encountered and it matches
the pair [a�l] being considered, the traversal stops. If the addlistener
call site a is never reached, this means that [a�l] is not
downward exposed and is not included in set A�c� o). In both of
our examples, the add-listener operation is downward exposed and
this step does not modify sets A�c� o).
Step 4 We construct a similar set R�c� o) of remove-listener operations.
However, only operations that are guaranteed to execute
along all possible execution paths should be included in this set. If
[r�l] could be avoided along some path, this could lead to a leak
of listener l. Thus, for each [r�l] observed in Step 2, we perform
a traversal of valid ICFG paths, starting from the entry node of c,
and stopping if [r�l] is encountered. If the exit node of c is reached,
this means that some valid ICFG path can avoid [r�l]. Set R�c� o)
excludes such remove-listener operations. In the running example,
the analysis of onDestroy will determine that each path through
the callback must reach the remove-listener call site, and therefore
this operation is included in R�c� o).
� Example: The final sets A and R computed by Phase 1 for the
two running examples are as follows:
A�onResume� DemoLauncher) = ∅
R�onResume� DemoLauncher) = ∅
A�onClick� br) =
� [requestLocationUpdates�EventManager] }
R�onClick� br) = ∅
A�onOptionsItemSelected� ai) = ∅
R�onOptionsItemSelected� ai) = ∅
A�onCreate� CheckinActivity) =
� [requestLocationUpdates�CheckinActivity] }
R�onCreate� CheckinActivity) = ∅
A�onDestroy� CheckinActivity) = ∅
R�onDestroy� CheckinActivity) =
� [removeUpdates�CheckinActivity] }
A�onStart/onResume/onPause� CheckinActivity) = ∅
R�onStart/onResume/onPause� CheckinActivity) = ∅
Here br and ai represent the static abstractions of the corresponding
run-time widgets. �
3.2 Phase 2: Path Generation
The second phase of the analysis creates a set of candidate paths
that represents the lifetime of an activity �for Pattern 1) or the
transition to a long-wait state �for Pattern 2). For each activity
w ∈ Win, we consider all incoming WTG edges t1 = [w��w]
that have push w as the last element in δ�t1). Starting from each
such t1, we perform a depth-first traversal to construct “candidate”
paths �t1� t2� . . . � tn�. The details of this traversal are presented in
Algorithm 1. During the traversal, path stores the current path and
stack is the window stack corresponding to that path.We only consider
paths whose length does not exceed some analysis parameter
k �in our implementation, this parameter’s value is 5). Any path
that represents a lifetime for the initial activity w or a transition to
a long-wait state from w is recorded for later processing.





第七页
Helper function ACTIVITYLIFETIME checks the following conditions:
�1) δ�tn) of the last edge tn in path contains pop w, and �2)
the stack operations in δ�tn), up to and including this pop w, when
applied to stack, result in an empty stack. The second condition
guarantees that the sequence of push/pop operations from push w
in δ�t1) to pop w in δ�tn) is a string in the language defined by
�alanced.
Helper function LONGWAIT determines if path will transit to a
long-wait state from the initial activity w. The following conditions
are checked: �1) stack is not empty and its top window w� is either
w or a dialog/menu owned by w, and �2) the event on the last
edge in path is [w��home]. Since the window stack is not empty,
the sequence of push/pop operations along path is a string in the
language defined by Valid .
During the depth-first traversal, helper function CANAPPEND
�invoked at line 16) considers the sequence δ�t) of stack operations
for a given edge t = [w�w�] and decides whether this sequence can
be successfully applied to the current window stack. In particular,
for each pop w�� operation in δ�t), the current top of the stack
must match w��. Furthermore, after all operations are applied, the
top of the stack must be the same as the target node of t. If
CANAPPEND returns true, it means that the sequence of stack
push/pop operations in the concatenation of δ�path) and δ�t) is
a string in the language defined by Valid .
If transition t is a valid extension of the current path, helper
function DOAPPEND appends t to path and applies stack operations
δ�t) to stack. After the traversal of the new path completes,
helper function UNDOAPPEND removes t from the path and “unrolls”
the changes made to stack due to operations δ�t).
� Example: Consider the example in Figure 2 and its WTG
shown in Figure 4a. Let a denote the WTG node for DemoLauncher.
Figure 4a shows transitions t1 = [launcher�a], t2 = [a�a], and
t3 = [a�launcher] with events ��t1) = launch, ��t2) = [br�click]
and ��t3) = [a�back]. In addition, consider transition t4 = [a�a]
with ��t4) = [a�home] �not shown in the figure). The stack operations
for these four edges are δ�t1) = push a, δ�t2) = [ ],
δ�t3) = pop a, and δ�t4) = [ ].
For the sake of the example, suppose we execute Algorithm 1
with k = 3. The candidate paths for Pattern 1 are �t1� t3�,
�t1� t2� t3�, and �t1� t4� t3�. The second path corresponds to the
problematic leaking behavior, as discussed earlier. The candidate
paths for Pattern 2 are �t1� t4� and �t1� t2� t4�. For the second path,
the callback sequence before the application goes in the background
is [onResume�a][onClick�br] �because no other lifecycle
callbacks are defined in the application), and therefore this is also
a leaking path. The next phase of the analysis considers all these
candidate paths and identifies the ones with leaking callback sequences.
�
3.3 Phase 3: Detection of Leaking Callback Sequences
In this phase, we perform leak detection on candidate paths
recorded in Phase 2. First, the relevant callback sequence is extracted
from each candidate path. Consider a transition sequence
T = �t1� . . . � tn� which represents a Pattern 1 candidate path. The
relevant callback subsequence of δ�T) is [c1�w] . . . [cm�w] where
c1 is the creation callback for w and cm is the termination callback
for w �w is the target window of edge t1). Similarly, for a sequence
T = �t1� . . . � tn� which is a Pattern 2 candidate path, the relevant
subsequence is [c1�w] . . . [cm�w�] where c1 is the creation callback
for w and cm is the last callback before the application goes in the
background.
Given a sequence of callbacks s = [c1�o1][c2�o2] . . . [cm�om],
we consider its sequence A1�A2� . . . �Am of add-listener sets and
R1�R2� . . . �Rm of remove-listener sets. Recall from Definition 1
that s leaks listener l if there exists an add-listener operation [a�l] ∈
Ai such that for each j > i there does not exist a matching
[r�l] ∈ Rj . In Phase 1, we have already computed sets A�c� o)
and R�c� o) for any relevant c and o. To detect leaks, we examine
each element [ci�oi] of s in order and maintain a set L of added
but not yet released listeners. Initially, L is empty. When [ci�oi] is
processed, all elements of R�ci� oi) are removed from L, and then
all elements of A�ci� oi) are added to L. Any [a�l] that remains in
L at the end of this process is considered to be a leak.
� Example: Consider again Figure 2 and the WTG in Figure 4a.
We have t1 = [launcher�a], t2 = [a�a] for the button click,
t3 = [a�launcher] for back, and t4 = [a�a] for home. Candidate
paths for Pattern 1 �for k = 3) are �t1� t3�, �t1� t2� t3�,
and �t1� t4� t3�. For the first and the third path, the relevant callback
sequence is [onResume�a] which is not leaking because
both A�onResume� a) and R�onResume� a) are empty. For the
second path, callback sequence [onResume�a][onClick�br] leaks
[requestLocationUpdates�EventManager]. For candidate path
�t1� t2� t4� for Pattern 2, the callbacks before the application goes
in the background are also [onResume�a][onClick�br] and there is
a leak as well.�
Defect reporting For any leaking candidate path �t1� . . .�, the
analysis records the pair [w�l] of the initial activity w �i.e., the
target of t1) and the leaking listener l, identified by the allocation
site of the corresponding object. For Figure 2, this would be
[DemoLauncher�l12] where l12 is the EventManager allocation
site at line 12 in the code. For each recorded pair, the leaking candidate
paths for that pair are also recorded. Each [w�l] is reported as
a separate defect, since it requires the programmer to examine the
callbacks associated with w and to determine whether they manage
listener l correctly.
Defect prioritization In addition to these reports, we also classify
leaking listeners as “high” or low “low” priority, based on the
following rationale. Consider again the example in Figure 3. The
leaking behavior can be observed only when a location read is
not obtained �e.g., the weather does not allow a GPS fix), which
arguably is not a very frequently-occurring situation. If we analyze
onLocationChanged—the callback that is executed on a listener
l when a location read is obtained—we can determine whether it
contains a remove-listener operation for l along each execution
第八页
path. If this is the case, a location read will release the listener. In
the defect report from the analysis such listeners are labeled as “low
priority”: they should still be examined by the programmer, but
perhaps after other leaking listeners have been examined. To make
this distinction, for each leaking l we analyze the corresponding
callback �onLocationChanged or similar method) using the same
approach as in Step 4 of Phase 1. The defect in Figure 3 will be
reported as low priority, while the one from Figure 2 will be high
priority. In our experiments 3 out of the 17 reported defects were
classified as low priority ones.
4. Evaluation
The static analysis was implemented in GATOR [41], our opensource
static analysis toolkit for Android. The toolkit contains implementations
of GUI structure analysis [40, 44] and WTG generation
[46–48]. The implementation of the energy defect analysis is
currently available as part of the latest release of GATOR.
The goal of our evaluation is to answer several questions. First,
how well does the analysis discover GUI-related energy-drain defects
already known from prior work? Second, does the analysis
discover defects that have not been identified in prior work? Third,
does the detection exhibit a reasonably small number of false positives?
Finally, what is the cost of the analysis?
Benchmarks To answer these questions, we used several sources
of benchmarks, as listed in Table 1. First, we considered the benchmarks
from the work on GreenDroid [27] that exhibit defects due
to incorrect control flow and listener operations in the UI thread
of the application. Almost all such GUI defects involve operations
related to location awareness, and our static analysis was built to
track add/release operations for location listeners. We also considered
the fixed versions of these benchmarks—that is, the versions
that involve fixes of these known defects. Both defective and fixed
versions were obtained from the public GreenDroid web site.3 In
Table 1, applications in the first part of the table are the defective
ones, while applications in the second part of the table, suffixed
with f,
are the ones with defect fixes.
We also considered the F-Droid repository of open-source applications
4 and searched for applications that use location-awareness
3 sccpu2.cse.ust.hk/greendroid
4 f-droid.org
capabilities in their UI-processing code. Specifically, the textual
description and the manifest file were checked for references to
location awareness of GPS, and the code was examined to ensure
that the UI thread uses location-related APIs. For the applications
we could successfully build and run on an actual device, the static
analysis was applied to detect potential defects. Out of the 10 applications
that were analyzed, 5 were reported by the analysis to
contain defects. The last part of Table 1 shows these 5 applications.
Columns “Nodes” and “Edges” show the numbers of WTG
nodes and edges, respectively. Column “Paths” contains the number
of candidate paths that were recorded and then analyzed for
leaking listeners. The last column in the table shows the cost of the
analysis; for this collection of experiments, this cost is very low.
Detected defects Recall that for a leaking path �t1� . . .�, the analysis
reports a pair [w�l] of the initial activity w �i.e., the target of
t1) and the leaking listener l. We consider each [w�l] to be a defect.
Column “Pat-1” shows the number of such defects that were
reported by the static analysis as instances of Pattern 1. Column
“Pat-2” shows a similar measurement for Pattern 2. In our experiments,
a total of 17 unique pairs [w�l] were reported, and all defects
that match Pattern 1 �11 defects) also match Pattern 2 �17 defects),
but not vice versa. However, it is still useful to detect both patterns
statically, as they correspond to two different scenarios. If a defect
matches both Pattern 1 and Pattern 2 �e.g., the one in Figure 2),
it usually means that the programmer completely ignored the removal
of the listener. On the other hand, if a defect matches Pattern
2 but not Pattern 1 �e.g., the one in Figure 3), this means that the
programmer attempted to remove the listener, but did not do it correctly.
Given the low cost of the analysis, we believe that detection
of both patterns is valuable.
Column “Real-1” shows the number of detected defects from
column “Pat-1” that we manually confirmed to be real, by observing
the actual run-time behavior of the application. Similarly, column
“Real-2” shows the number of defects from column “Pat-2”
that were verified in the same manner. Only one reported defect is
incorrect: in wigle, a defect is incorrectly reported by the analysis
as an instance of both Pattern 1 and Pattern 2, while in reality it is
only an instance of Pattern 2. The cause of this imprecision will be
discussed shortly.
Two conclusions can be drawn from these measurements. First,
the analysis successfully detects various defects across the ana-
lyzed applications. Even the “fixed” versions are not free of defects:
for example, we discovered two defects in ushahidif
that
were not reported in the work on GreenDroid, and were missed
by the application developers when ushahidi was fixed to obtain
ushahidif
�in fact, these two defects are quite similar to the one
that was fixed). A similar situation was observed for droidar. This
observation indicates the benefits of static detection, compared to
run-time detection which depends on hard-to-automate triggering
of the problematic behavior. Of course, static detection has it own
limitations, with the main one being false positives. However, the
experimental results for the 15 benchmarks shown in Table 1 indicate
that the proposed analysis achieves very high precision.
False positive The false positive for wigle is because the developer
decided to override standard method Activity.finish
with a custom version which removes the listener. When method
finish is invoked on an activity �by the application code or by the
framework code), this causes the termination of the activity. However,
this method is not a callback that is defined as part of the
lifecycle of an activity, and is rarely overridden by applications. In
other words, finish can be called to force termination, but it is not
executed as part of the actual termination process. Thus, finish
does not appear on WTG edges �although it is accounted for during
WTG creation [48]). In fact, termination could happen even
if finish is not called: for example, the system may silently terminate
an activity to recover memory [14]. The Android lifecycle
model guarantees that onDestroy will be called in all scenarios,
and this is where the listener should be removed, rather than in
finish. This example indicates that the developer misunderstands
the activity lifecycle. During the manual examination of this defect
on a real device we did observe that the location listener is properly
released, and decided to classify the defect as a false positive,
although one could argue that it violates Android guidelines.
Comparison with GreenDroid To compare the proposed static
detection with the most relevant prior work, Table 4 considers the
UI thread defects [w�l] reported by the dynamic analysis approach
in GreenDroid. For a given application, let D be the set of these
defects, while S be the set of defects reported by our static analysis.
The sizes of these sets are given in the second and third column in
Table 4. The next two columns show the sizes of sets D−S and
S−D, respectively. As the next-to-last column shows, our static
analysis reported all defects from the prior dynamic analysis work.
The last column shows how many of the statically-detected defects
were not reported by GreenDroid; for two of the applications, there
are additional defects we discovered statically �and these defects
are still present in the fixed versions from GreenDroid). A possible
explanation of this result is that the run-time exploration strategy
in GreenDroid did not trigger the necessary GUI events; in general,
comprehensive run-time GUI coverage is challenging [8].
Overall, these results indicate that static detection could be more
effective than dynamic detection. At the same time, it is important
to consider the relative strengths and weaknesses of both approaches:
while the static analysis can model more comprehensively
certain behaviors of the UI thread, other aspects of runtime
semantics are not modeled statically �e.g., asynchronous tasks
and services) and dynamic analysis does capture additional defects
for such behaviors. This highlights the need for more comprehensive
static control-flow analyses for Android, as well as hybrid
static/dynamic approaches for defect detection.
Summary On 15 Android applications, the proposed analysis
exhibits low cost and high precision. It discovers all GUI-related
leaking-listener defects discovered by GreenDroid, as well as three
new ones. Additional defects were discovered in 5 applications not
analyzed in prior work. With one exception, all reported defects
are observable at run time. These results indicate that the analysis
is suitable for use in real-world static checking tools.
5. RelatedWork
Energy analysis for Android There is a growing body of work
on analyzing and optimizing the energy consumption of mobile devices.
Studies have shown [6, 25] that battery drain issues on an Android
device could be caused by poorly managed background activities
and inappropriate invocations to energy-related APIs. Qian
et al. [38] developed a resource usage profiler to uncover inefficient
usage of radio and network resources. Some energy optimizers
[22, 26] could reduce the energy consumption when an application
is displayed on OLED screens, by modifying the color scheme;
these techniques do not consider any energy-related defects. Oliner
et al. [33] used statistical battery discharge rate data of smartphone
applications in a community of devices in order to uncover battery
draining applications. A similar approach is used by Min et al.
[30]. Though this method reveals accurate results, it requires runtime
data collected from a large number of devices that have the
target application installed, which is not always possible.
An approach from Zhang et al. [49] uses dynamic taint analysis
to detect design flaws related to network operations. However,
this approach introduces relatively high run-time overhead and cannot
be used to analyze other energy defects. Early work of energyaware
profiling [19, 21, 34, 35] used dynamic analysis to detect energy
hotspots in Android applications. However, as with other program
profiling techniques, they require comprehensive test cases
to execute the application on the device. Furthermore, an energy
hotspot may not always point to the underlying energy-related defect.
For example, if there is an energy leak due to mismanagement
of lifecycle callbacks, these callbacks �which are typically short
lived) are unlikely to be reported as hotspots.
Following their work on energy profiling [34, 35], Pathak et al.
defined a static analysis to detect energy-related defects [36]. This
analysis aims to model control flow along multiple threads of execution
and to detect code paths for which energy-related resources
�e.g., location listeners and wake locks) are not properly released.
The control-flow modeling is very simplistic: while some simple
sequences of lifecycle callbacks are considered in the analysis,
there is no modeling of interleaving of callbacks across multiple
windows, nor is there modeling of the window stack. Furthermore,
the approach requires human involvement: the developer
is expected to specify expected invocation orders of widget event
handlers, which is impractical and may lead to limited coverage of
possible behaviors. In our approach, the order of callbacks in the UI
thread is determined automatically through a more general and precise
analysis. The vast majority of defects in this work �e.g., wake
lock leaks) are reported for service components running concurrently
with the UI thread. There are only two applications for which
location-awareness defects are reported. One defect is in ushahidi
�already discussed earlier); the same defect is discovered by Green-
Droid and by our approach, but we also detect two other similar
defects not reported by [36] or by GreenDroid. The other case is an
application whose code is not available anymore �dead URL) and
