:- [indigolog-vanilla].

%
% Fluents of the domain
% 
dom_fluent(a).
dom_fluent(b).
dom_fluent(c).
dom_fluent(d).
dom_fluent(e).
dom_fluent(f).

%
% Axiomatizatoin of the actions
% 
prim_action(A) :- dom_action(A).

dom_action(a111).
poss(a111, and(a=true,and(b=true, c=true))).

dom_action(a112). 
poss(a112, and(a=true,and(b=true, c=false))).
causes_val(a112, c, true, true).

dom_action(a121). 
poss(a121, and(c=true,and(d=true,f=true))).
causes_val(a121, e, true, true).

dom_action(a122). 
poss(a122, and(c=true,and(d=true, f=false))).

dom_action(a131). 
poss(a131 ,or(e=true,f=true)).

dom_action(a132). 
poss(a132, and(e=false, f=false) ).

dom_action(a211). 
poss(a211, and(a=false,
	   and(d=false,
	       or(b=true, c=true)))
    ).

dom_action(a212). 
poss(a212, and(a=false,
	   and(d=false,
	       or(b=false, c=false)))
    ).

dom_action(a221).
poss(a221,e). 

dom_action(a222).
poss(a222, e=false).

dom_action(a231).
poss(a231, and(a=false,
	   and(d=false,
	       and(b=true,f=true)))).

dom_action(a232). 
poss(a232, and(a=false,
	   and(d=false,
	       or(b=false,
		  f=false)))).

dom_action(a311). 
poss(a311, 
	and(or(b=false,or(and(b=true,a=false),and(b=true,d=false))),
    	or(c=true,or(e=true,f=true)) )
).

dom_action(a312).
poss(a312, 
		and(
			or(b=false, 
			or(and(b,a=false),
			   and(b,d=false)
			   )),
	         c=false)
	         ).




% Axiomatization of the initial state (no use for us though)
initially(a,true).
initially(b,true).
initially(c,true).
initially(d,true).
initially(e,true).
initially(f,true).


	
%
% Finally the encoding of the goal-plan hierarachy
% 
rule(top, [g11,g12,g13]).
rule(top, [g21,g22,g23]).
rule(top, [g31]).

rule(g11, [a111]).
rule(g11, [a112]).

rule(g12, [a121]).
rule(g12, [a122]).

rule(g13, [a131]).
rule(g13, [a132]).

rule(g21, [a211]).
rule(g21, [a212]).

rule(g22, [a221]).
rule(g22, [a222]).

rule(g23, [a231]).
rule(g23, [a232]).

rule(g31, [a311]).
rule(g31, [a312]).





%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%% End of the domain axiomatization
%% what follows is just code to do the testing -- not very neat
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

% top-level procedure to build a legal plan P for each world S
testAll :-
	build_world(S),
	(find_plan(S,P) -> 
		write(S), write('  ---> ' ), writeln(P),
		append(P,S,PS),
		writeln(PS),nl 
	; 
		writeln(S)),
	fail.
testAll :- writeln('All done!').

solvableWorld(S,P) :-
	build_world(S),
	once(find_plan(S,P)).

unsolvableWorld(S) :-
	build_world(S),
	\+ find_plan(S,_).
	
	


% Build a legal world S
build_world(S) :-
	findall(set(F,_),  dom_fluent(F), LF),
	set_truth_values(LF,S).
	
set_truth_values([],[]).
set_truth_values([set(F,_)|L],[set(F,V)|L2]) :-
	member(V,[true,false]),
	set_truth_values(L,L2).
	

% Find a legal plan Plan for world S
find_plan(S,Plan) :-     
	goalToPlan(top, Plan),
	append(Plan,S,History),
	check_valid(History).

% check a plan is valid (i.e., all prec of actions hold)
check_valid([]).
check_valid([A|H]) :-
	poss(A,C),
	holds(C,H),
	check_valid(H).




% Return a legal plan P for a given goal G
goalToPlan(G, P2) :-
	rule(G, P1),
	expand_plan(P1, P),
	reverse(P,P2).

% Expand a plan to the level of all prim actions
expand_plan([], []).
expand_plan([A|L], [A|L2]) :- 
	prim_action(A), !,
	expand_plan(L,L2).
expand_plan([G|L], L3) :- 
	goalToPlan(G,LG),
	expand_plan(L,L2),
	append(LG,L2,L3).

	
%
% Other stuff....
% 
execute(A,Sr) :- ask_execute(A,Sr).
exog_occurs(_) :- fail.

prim_action(A) :- dom_action(A).
prim_fluent(F) :- dom_fluent(F).

prim_action(set(_,_)).
poss(set(_,_), true).
causes_val(set(F,V), F, V, true).
	