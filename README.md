Callbacks Each e ∈ Event triggers a sequence of callback
invocations that can be abstracted as [c1�o1][c2�o2] . . . [cm�om].
Here ci is a callback method defined by the application, and oi
is a run-time object on which ci was triggered. Note that each
of these invocations completes before the next one starts—that is,
their lifetimes are not nested within each other, but rather they are
disjoint. The actual invocations are performed by event-processing
logic implemented inside the Android framework code.
We consider two categories of callbacks. Widget event handler
callbacks respond to widget events; an example is onClick in Figure
2. Lifecycle callbacks are used for lifetime management of
windows. For example, creation callback onCreate indicates the
start of the activity’s lifetime, and termination callback onDestroy
indicates end of lifetime. Menus and dialogs can also have create/
terminate callbacks.
� Example: In Figure 2 event [br�click ] �br is the Button
object referenced by btnRun) will cause a widget event handler
callback invocation [onClick�br]. In this example the callback
sequence contains only this invocation. However, for the sake
of the example, suppose that onClick invoked an Android API
call to start some new activity a. Also, for illustration, suppose
that the source activity DemoLauncher and the target activity a
both define the full range of activity lifecycle callbacks �listed for
completeness at lines 6–7 in Figure 2). Then the callback invocation
sequence would be [onClick�br][onPause�DemoLauncher]
[onCreate�a][onStart�a][onResume�a][onStop�DemoLauncher];
this sequence can be observed via android.os.Debug tracing.
If after [br�click ] the next event was [a�back]—that is, the
BACK button was pressed to close a and return to DemoLauncher—
the sequence would be [onPause�a] [onRestart�DemoLauncher]
[onStart�DemoLauncher][onResume�DemoLauncher][onStop�a]
[onDestroy�a]. As seen from these examples, there can be a nontrivial
sequence of callback invocations in response to a single GUI
event. �
Window transitions We use the term run-time window transition
to denote a pair t = [w�w�] ∈ Win × Win showing that when
window w was active and interacting with the user, a GUI event
occurred that caused the new active window to be w� �w� may
be the same as w). Each transition t is associated with the event
��t) ∈ Event that caused the transition and with σ�t), a sequence
of callback invocations [ci�oi].
There are two categories of callback invocation sequences for
Android GUI transitions. The first case is when event ��t) is a
widget event [v�k] where v is a widget in the currently-active
window w. In this case σ�t) starts with [c1�v] where c1 is the
callback responsible for handling events of type k on v. The rest
of the sequence contains [ci�wi] with ci being a lifecycle callback
on some window wi. In general, the windows wi whose lifecycles
are affected include the source window w, the target window w�, as
well as other related windows �e.g., the owner activity of w). In the
running example, a self-transition t for DemoLauncher is triggered
by event [br�click ], resulting in σ�t) = [onClick�br]. Following
the hypothetical example from above, if onClick opens another
activity a, the transition would be from DemoLauncher to a, with
σ�t) as listed above: [onClick�br] . . . [onStop�DemoLauncher].
The second category of callback sequences is when ��t) is a default
event [w�k] on the current window w. In this case all elements
of σ�t) involve lifecycle callbacks. For example, event home on
DemoLauncher triggers a self-transition t with σ�t) containing invocations
of onPause� onStop� onRestart� onStart� onResume
on that activity. Additional details of the structure of these callback
sequences are presented in our earlier work [46, 48].
Window stack Each transition t may open new windows and/or
close existing ones. This behavior can be modeled with a window
stack: the stack of currently-active windows.2 Each transition
t can modify the stack by performing window push/pop sequences.
These effects will be denoted by δ�t) ∈ ��push� pop} ×Win)�.
In the examples presented in this paper, the effects of a transition t
are relatively simple: for example, opening a new window w represented
by push w, or closing the current window w represented by
pop w. In the simplest case, as in the self-transition t from Figure 2,
δ�t) is empty. However, our prior work [48] shows that in general
these effects are more complex: δ�t) could be a �possibly empty)
sequence of window pop operations, followed by an optional push
operation. These operations could involve several windows and can
trigger complicated callback sequences.
Transition sequences Consider any sequence of transitions T =
�t1� t2� . . . � tn� such that the target of ti is the same as the source of
ti�1. Let σ�T) be the concatenation of callback sequences σ�ti);
similarly, let δ�T) be the concatenation of window stack update
sequences δ�ti). Sequence T is valid if δ�T) is a string in a
standard context-free language [39] defined by
Valid → �alanced Valid | push wi Valid | �
where �alanced describes balanced sequences of matching push
and pop operations
�alanced → �alanced �alanced | push wi �alanced pop wi | �
2.2 Adding and Removing of Listeners
The callbacks invoked during window transitions can perform a variety
of actions. Our work considers actions that may affect energy
consumption. In particular, we focus on add-listener and removelistener
operations related to location awareness. Such actions have
been considered by GreenDroid [27], an existing dynamic analysis
tool for detection of energy defects in Android applications. Since
almost all GUI-related energy-drain defects reported in this prior
work are due to location awareness, focusing on such defects allows
us to perform direct comparison with the results from this
existing study.
