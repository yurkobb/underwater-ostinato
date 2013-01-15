////////////
// Studio //
////////////
(
SynthDef(\padsA, {
	|out = 0, amp = 1, gate = 1, freq = 440, transitionDuration = 0.1, lfoFreq = 0.2 |
	var generator = SinOsc.ar(Lag2.kr(freq, transitionDuration), 0, amp) + Impulse.ar(Lag3.kr(freq, transitionDuration * 0.9) * 2, 0.025);
	var envelope = Linen.kr(gate, releaseTime: transitionDuration / 2, doneAction: 2);
	var lfo = SinOsc.kr([lfoFreq, lfoFreq * 0.7], 0, 0.1, 0.9);
	var filterLfo = SinOsc.kr([lfoFreq * 0.3, lfoFreq * 0.35], 0, 0.25, 0.9);
	var filter = BPF.ar(generator * lfo, Lag.kr(freq, transitionDuration) * filterLfo, rq: 1);
	var ampUgen = Lag2.kr(amp, transitionDuration);
	var synthesized = filter * envelope * ampUgen;

	Out.ar(out, synthesized);
}).add;
SynthDef(\reverb, {
	|out = 0, in = 0, amp = 1, mix = 0.33, room = 0.5, damp = 0.5, feedbackMul = 0.85 |
	var inputSignal, localIn, feedback, reverb;
	inputSignal = In.ar(in, 2);
	LocalOut.ar(CombN.ar(inputSignal));
	localIn = LocalIn.ar(2);
	feedback = localIn * feedbackMul + inputSignal;
	reverb = FreeVerb2.ar(feedback[0], feedback[1], mix, room, damp);
	//reverb = feedback;
	Out.ar(out, reverb);
}).add;
)
(
~reverbBus = Bus.audio(Server.default, 2);
~reverb = Synth(\reverb, [\in, ~reverbBus, \damp, 0.1, \room, 0.8, \mix, 0.5]);
//~reverb.free;
)
///////////////
// Constants //
///////////////
(
// Series a
~seriesA = [0, 7, 10, 12];
~proportionsA = [(7 / 12), (7 / 10), 1, (10 / 7), (12 / 7)];
~chordsA = [
	[0, 4, 9, 13],
	[0, 4, 9, 12],
	[0, 4, 5, 9],
	[0, 3, 4, 9],
	[0, 2, 3, 8],
	[0, 2, 3, 6],
	[0, 1, 4, 9],
	[0, 1, 4, 7],
	[0, 2, 4, 5],
	[0, 2, 4, 12]
];

// Sequence a
~sequenceA = [
	// Each step consists of a chord as an index from the ~chordsA array
	// and transposition in Pbind degrees
	[0, 0],
	[1, -2],
	[2, 0],
	[3, 0],
	[4, 2],
	[5, 2],
	[6, 4],
	[2, 4],
	[3, -2],
	[6, -8],
	[7, -8],
	[7, -10],
	[8, -8],
	[9, -4],
	[0, 0]
];
)

(
~padsASequence = Routine({
	var duration, durationMultiplier, chord, transposition, ppar, amp, startNote;
	durationMultiplier = Pseq(~proportionsA, inf).asStream;
	~sequenceA.do({ |step|
		amp = 0.25;
		startNote = 4.rand;
		durationMultiplier = durationMultiplier.next * 3;
		duration = 14 * durationMultiplier;
		chord = ~chordsA[step[0]];
		transposition = step[1];
		
		ppar = Ppar([
			Pmono(
				\padsA,
				\dur, Pseq([1, duration]),
				\degree, Pseq([chord[startNote], chord[0]]),
				\mtranspose, transposition,
				\out, ~reverbBus,
				\amp, Pseq([0, amp]),
				\transitionDuration, ~seriesA[1] * durationMultiplier
			),
			Pmono(
				\padsA,
				\dur, Pseq([1, duration]),
				\degree, Pseq([chord[startNote], chord[1]]),
				\transitionDuration, ~seriesA[1] * durationMultiplier,
				\mtranspose, transposition,
				\amp, Pseq([0, amp]),
				\out, ~reverbBus
			),
			Pmono(
				\padsA,
				\dur, Pseq([1, duration]),
				\degree, Pseq([chord[startNote], chord[2]]),
				\transitionDuration, ~seriesA[2] * durationMultiplier,
				\mtranspose, transposition,
				\amp, Pseq([0, amp]),
				\out, ~reverbBus
			),
			Pmono(
				\padsA,
				\dur, Pseq([1, duration]),
				\degree, Pseq([chord[startNote], chord[3]]),
				\transitionDuration, ~seriesA[3] * durationMultiplier,
				\mtranspose, transposition,
				\amp, Pseq([0, amp]),
				\out, ~reverbBus
			)
		]);
		ppar.play;
		duration.wait;
	})
});
~padsASequencePlayer = Routine({
	var interval, routine;
	interval = ~seriesA * 8;
	loop {
		interval.do({ |dur|
			if(dur.booleanValue, {
				routine = ~padsASequence.reset.play;
				"Next cycle.".postln;
				dur.wait;
			});
		})
	}
});
)
(
b = ~padsASequencePlayer.reset.play;
b.stop;