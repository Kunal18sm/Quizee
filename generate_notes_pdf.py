# -*- coding: utf-8 -*-
import os
import subprocess

html_content = """<!DOCTYPE html>
<html lang="hi">
<head>
<meta charset="UTF-8">
<title>SSC CHSL Tier-1 Master Revision Notes</title>
<style>
  @page {
    size: A4;
    margin: 15mm 15mm 15mm 15mm;
    @bottom-right {
      content: counter(page);
    }
  }
  body {
    font-family: 'Segoe UI', Arial, sans-serif;
    color: #222;
    line-height: 1.5;
    font-size: 13px;
    background: #fff;
  }
  h1.doc-title {
    text-align: center;
    color: #1a365d;
    font-size: 24px;
    margin-bottom: 4px;
    border-bottom: 3px solid #2b6cb0;
    padding-bottom: 8px;
  }
  .doc-subtitle {
    text-align: center;
    color: #4a5568;
    font-size: 13px;
    font-weight: 600;
    margin-bottom: 20px;
  }
  .section-banner {
    background: #2b6cb0;
    color: #fff;
    padding: 8px 14px;
    font-size: 16px;
    font-weight: bold;
    border-radius: 4px;
    margin-top: 24px;
    margin-bottom: 12px;
    page-break-after: avoid;
  }
  .section-banner.maths { background: #c05621; }
  .section-banner.reasoning { background: #6b46c1; }
  .section-banner.gk { background: #276749; }

  h2 {
    color: #2c5282;
    font-size: 15px;
    margin-top: 16px;
    margin-bottom: 8px;
    border-left: 4px solid #3182ce;
    padding-left: 8px;
    page-break-after: avoid;
  }
  .maths h2 { color: #9c4221; border-color: #dd6b20; }
  .reasoning h2 { color: #553c9a; border-color: #805ad5; }
  .gk h2 { color: #22543d; border-color: #38a169; }

  h3 {
    font-size: 13.5px;
    color: #2d3748;
    margin-top: 10px;
    margin-bottom: 4px;
  }

  p { margin: 4px 0 8px 0; }
  ul, ol { margin: 4px 0 10px 20px; padding: 0; }
  li { margin-bottom: 4px; }

  .rule-box {
    background: #ebf8ff;
    border: 1px solid #bee3f8;
    border-left: 5px solid #3182ce;
    padding: 8px 12px;
    border-radius: 4px;
    margin: 8px 0;
    page-break-inside: avoid;
  }
  .rule-box.maths {
    background: #fffaf0;
    border-color: #feebc8;
    border-left-color: #dd6b20;
  }
  .rule-box.reasoning {
    background: #faf5ff;
    border-color: #e9d8fd;
    border-left-color: #805ad5;
  }
  .rule-box.gk {
    background: #f0fff4;
    border-color: #c6f6d5;
    border-left-color: #38a169;
  }

  .pyq-box {
    background: #f7fafc;
    border: 1px dashed #cbd5e0;
    padding: 8px 12px;
    margin: 6px 0;
    border-radius: 4px;
    font-size: 12.5px;
    page-break-inside: avoid;
  }
  .pyq-title {
    font-weight: bold;
    color: #b7791f;
    margin-bottom: 4px;
  }

  table {
    width: 100%;
    border-collapse: collapse;
    margin: 10px 0;
    font-size: 12px;
    page-break-inside: avoid;
  }
  th, td {
    border: 1px solid #cbd5e0;
    padding: 6px 8px;
    text-align: left;
  }
  th {
    background: #edf2f7;
    color: #2d3748;
    font-weight: bold;
  }
  tr:nth-child(even) { background: #f7fafc; }

  .diagram-container {
    text-align: center;
    margin: 10px 0;
    page-break-inside: avoid;
  }
  svg { max-width: 100%; height: auto; }
  .badge {
    display: inline-block;
    padding: 2px 6px;
    font-size: 11px;
    font-weight: bold;
    color: white;
    background: #e53e3e;
    border-radius: 3px;
    margin-right: 4px;
  }
  .formula {
    font-family: 'Cambria Math', Georgia, serif;
    font-size: 14px;
    font-weight: bold;
    color: #c53030;
  }
  .page-break { page-break-before: always; }
</style>
</head>
<body>

<h1 class="doc-title">SSC CHSL TIER-1 : MASTER REVISION NOTES</h1>
<div class="doc-subtitle">Based on Detailed Multi-Year Analysis of Official PYQs (2017 to 2024)<br>Focus: 100% Guaranteed Common Question Types & Smart Study Rules</div>

<!-- ======================================================== -->
<!-- SECTION 1: ENGLISH LANGUAGE -->
<!-- ======================================================== -->
<div class="section-banner">SECTION 1 : ENGLISH LANGUAGE (BASIC KNOWLEDGE)</div>

<h2>1. Article Rules (Har Shift Me 1 Se 2 Questions Pakke)</h2>
<div class="rule-box">
  <b>Rule 1: Indefinite Article 'A' vs 'An' (Sound Based)</b><br>
  Article 'A' aur 'An' ka use letter dekh kar nahi, balki <b>Vowel Sound (स्वर ध्वनि: अ, आ, इ, ई, ए, ऐ, ओ, औ)</b> ke aadhar par hota hai.<br>
  • Agar shabd ka pehla uchcharan (pronunciation) Vowel Sound hai $\rightarrow$ <b>'An'</b> (e.g., <i>an umbrella, an American, an honest man, an hour, an heir</i>).<br>
  • Agar uchcharan Consonant Sound (व्यंजन ध्वनि: क, ख, ग, य, व...) hai $\rightarrow$ <b>'A'</b> (e.g., <i>a European</i> [sound: 'yu'], <i>a university</i> [sound: 'yu'], <i>a one-eyed man</i> [sound: 'va']).
</div>

<div class="pyq-box">
  <div class="pyq-title">PYQ Repetition Examples:</div>
  • <b>(CHSL 2024 S2):</b> "This is a doll and there is <u>a umbrella</u>." $\rightarrow$ <b>Correction:</b> <i>an umbrella</i> (umbrella starts with 'अ' sound).<br>
  • <b>(CHSL 2024 S2):</b> "Emily is <u>a American</u> living in Paris." $\rightarrow$ <b>Correction:</b> <i>an American</i>.<br>
  • <b>(CHSL 2018 S1):</b> "A stream is <u>unbroken whole</u>..." $\rightarrow$ <b>Correction:</b> <i>an unbroken whole</i>.
</div>

<div class="rule-box">
  <b>Rule 2: Definite Article 'The' ke 3 Most-Repeated Cases</b><br>
  1. <b>Superlative Degree:</b> Superlative adjective se pehle hamesha 'The' lagta hai.<br>
     • <i>PYQ (CHSL 2024 S1):</i> "This is <u>best book</u> for children." $\rightarrow$ <b>Correction:</b> <i>This is <b>the best book</b></i>.<br>
  2. <b>Phrase "One of the + Plural Noun":</b><br>
     • <i>PYQ (CHSL 2020 S1):</i> "When I met Akbar Padamsee, <u>one of a pioneers</u>..." $\rightarrow$ <b>Correction:</b> <i>one of <b>the</b> pioneers</i>.<br>
  3. <b>Unique / Defined Objects & Holy/Geographical entities:</b> <i>The Sun, The Moon, The Himalayas, The Ganga, The Philippines</i> (islands group).
</div>

<h2>2. Subject-Verb Agreement & Concord (Most Crucial)</h2>
<div class="rule-box">
  • <b>Rule A: Singular Subject $\rightarrow$ Singular Verb ($V_1 + s/es$)</b><br>
    <i>PYQ (CHSL 2024 S2):</i> "It is a general belief that the female ostrich <u>guard</u> the nest..." $\rightarrow$ <b>Correction:</b> <i>female ostrich <b>guards</b></i>.<br>
    <i>PYQ (CHSL 2020 S1):</i> "Your skill <u>lie</u> in cheering up people..." $\rightarrow$ <b>Correction:</b> <i>Your skill <b>lies</b></i>.<br>
    <i>PYQ (CHSL 2021 S1):</i> "The Prime Minister <u>chair</u> a high-level meeting..." $\rightarrow$ <b>Correction:</b> <i>The Prime Minister <b>chairs / chaired</b></i>.<br>
  • <b>Rule B: Plural Subject $\rightarrow$ Plural Verb</b><br>
    <i>PYQ (CHSL 2017 S1):</i> "Due to the snow the <u>marks was</u> unrecognisable." $\rightarrow$ <b>Correction:</b> <i>marks <b>were</b></i>.<br>
  • <b>Rule C: "One of the" + Plural Noun + Singular Verb</b><br>
    <i>PYQ (CHSL 2021 S1):</i> "Public speaking is one of <u>the most feared form</u>..." $\rightarrow$ <b>Correction:</b> <i>most feared <b>forms</b></i>.
</div>

<h2>3. Adverb vs Adjective Placement Errors</h2>
<div class="rule-box">
  • <b>Rule:</b> Verb ko modify karne ke liye <b>Adverb (-ly form)</b> lagta hai, Adjective nahi.<br>
    <i>PYQ (CHSL 2024 S1):</i> "Passengers started behaving <u>violent</u> when..." $\rightarrow$ <b>Correction:</b> <i>behaving <b>violently</b></i>.<br>
    <i>PYQ (CHSL 2022 S3):</i> "Rahul sings <u>very sweet</u> when..." $\rightarrow$ <b>Correction:</b> <i>sings <b>very sweetly</b></i>.<br>
  • <b>Rule: Placement of 'Enough':</b><br>
    Adjective ke <b>baad</b> 'enough' lagta hai: [Adjective + Enough].<br>
    <i>PYQ (CHSL 2024 S2):</i> "The hotel room <u>isn't enough big</u>." $\rightarrow$ <b>Correction:</b> <i>isn't <b>big enough</b></i>.
</div>

<h2>4. Preposition & Conjunction Rules</h2>
<div class="rule-box">
  • <b>Between vs Among:</b> 2 ke liye <i>Between</i>, 3 ya usse zyada ke liye <i>Among</i>.<br>
    <i>PYQ (CHSL 2022 S3):</i> "divide the expenses <u>between the three of us</u>" $\rightarrow$ <b>Correction:</b> <i><b>among</b> the three of us</i>.<br>
  • <b>Unless vs Until:</b> <i>Unless</i> = Condition (shart), <i>Until</i> = Time (samay). Dono me negative shabd (not) nahi judta.<br>
    <i>PYQ (CHSL 2021 S1):</i> "I cannot help you <u>if you tell me</u> your problem." $\rightarrow$ <b>Correction:</b> <i><b>unless</b> you tell me</i>.
</div>

<h2>5. Active-Passive Voice & Direct-Indirect Speech (100% Fixed Format)</h2>
<div class="rule-box">
  <b>Voice Shortcuts:</b><br>
  • Simple Past ($V_2$) $\rightarrow$ $was/were + V_3$ [e.g., <i>"The police beat protestors" $\rightarrow$ "Protestors were beaten"</i>].<br>
  • Imperative (Orders/Offers): <i>"Celebrate the New Year" $\rightarrow$ "<b>Let</b> the New Year <b>be celebrated</b>"</i>.<br>
  <b>Narration Shortcuts:</b><br>
  • Interrogative (Yes/No Question): Reporting verb <i>said to $\rightarrow$ asked</i>, connector <i>if / whether</i>, sentence assertive ho jata hai.<br>
  • Universal Truths / Proverbs: Tense change nahi hota [e.g., <i>The sage said, "Man proposes and God disposes" $\rightarrow$ The sage said that man proposes and God disposes</i>].<br>
  • Polite Requests: <i>"Please help me" $\rightarrow$ requested + to + $V_1$</i>.
</div>

<h2>6. High Frequency Vocabulary & Spelling Mistakes</h2>
<table>
  <tr><th>Category</th><th>Word & Meaning</th><th>Exam Trend / Note</th></tr>
  <tr><td><b>OWS (Repeated)</b></td><td><b>Kleptomaniac:</b> Impulse to steal<br><b>Misogamist:</b> Hates marriage<br><b>Abdicate:</b> Renounce throne / power<br><b>Emigrant:</b> Leaves country to settle abroad<br><b>Aviary:</b> Place where birds are kept</td><td>Har saal 2 questions OWS aate hain. Common themes: Haters, Lovers, Government types, Living places.</td></tr>
  <tr><td><b>Idioms (Repeated)</b></td><td>• <b>Cost an arm and a leg:</b> Very expensive<br>• <b>At daggers drawn:</b> Ready to fight / hostile<br>• <b>Sell like hot cakes:</b> Sold very quickly<br>• <b>Up in arms:</b> Angry and protesting<br>• <b>Stir up a hornet's nest:</b> Provoke serious trouble</td><td>90% idioms previous year papers se direct repeat hote hain.</td></tr>
  <tr><td><b>Spelling Traps</b></td><td>• <b>Receive</b> (not recieve - rule: 'i before e except after c')<br>• <b>Committee</b> (double m, double t, double e)<br>• <b>Argument</b> (no 'e' after u)<br>• <b>Conscientious</b> (sci-en-tious)<br>• <b>Underrated</b> (double r)</td><td>Hamesha double consonants (mm, tt, rr, ss) aur 'ie/ei' par focus karein.</td></tr>
</table>

<div class="page-break"></div>

<!-- ======================================================== -->
<!-- SECTION 2: QUANTITATIVE APTITUDE -->
<!-- ======================================================== -->
<div class="section-banner maths">SECTION 2 : QUANTITATIVE APTITUDE (MATHS FORMULAS & SHORTCUTS)</div>

<h2>1. Geometry: Circle Tangents & Chords (Har Paper Ka Favorite)</h2>

<div class="diagram-container">
  <!-- SVG Diagram for Direct and Transverse Tangents -->
  <svg width="520" height="150" viewBox="0 0 520 150">
    <!-- Direct Common Tangent -->
    <g transform="translate(10, 10)">
      <circle cx="50" cy="65" r="45" fill="#f7fafc" stroke="#2b6cb0" stroke-width="2"/>
      <circle cx="180" cy="65" r="25" fill="#f7fafc" stroke="#2b6cb0" stroke-width="2"/>
      <line x1="50" y1="65" x2="180" y2="65" stroke="#718096" stroke-dasharray="4"/>
      <line x1="50" y1="20" x2="180" y2="40" stroke="#e53e3e" stroke-width="2.5"/>
      <text x="115" y="15" fill="#e53e3e" font-size="11" font-weight="bold">Direct Common Tangent (DCT)</text>
      <text x="85" y="80" fill="#4a5568" font-size="11">d = distance</text>
      <text x="50" y="68" font-size="10" text-anchor="middle">r₁</text>
      <text x="180" y="68" font-size="10" text-anchor="middle">r₂</text>
    </g>
    <!-- Transverse Common Tangent -->
    <g transform="translate(280, 10)">
      <circle cx="50" cy="65" r="40" fill="#fffaf0" stroke="#c05621" stroke-width="2"/>
      <circle cx="180" cy="65" r="25" fill="#fffaf0" stroke="#c05621" stroke-width="2"/>
      <line x1="50" y1="65" x2="180" y2="65" stroke="#718096" stroke-dasharray="4"/>
      <line x1="38" y1="27" x2="192" y2="88" stroke="#dd6b20" stroke-width="2.5"/>
      <text x="115" y="15" fill="#dd6b20" font-size="11" font-weight="bold">Transverse Tangent (TCT)</text>
      <text x="50" y="68" font-size="10" text-anchor="middle">r₁</text>
      <text x="180" y="68" font-size="10" text-anchor="middle">r₂</text>
    </g>
  </svg>
</div>

<div class="rule-box maths">
  <b>1. Direct Common Tangent (DCT) Length:</b>
  <div class="formula">L_{DCT} = \sqrt{d^2 - (r_1 - r_2)^2}</div>
  <i>Yahan $d$ dono kendron (centers) ke bich ki duri hai, aur $r_1, r_2$ radii hain.</i><br>
  <b>PYQ (CHSL 2023 S1 Q64):</b> $r_1 = 12\text{ cm}, r_2 = 5\text{ cm}, d = 25\text{ cm}$.<br>
  $\implies L_{DCT} = \sqrt{25^2 - (12 - 5)^2} = \sqrt{625 - 49} = \sqrt{576} = \mathbf{24\text{ cm}}$.
</div>

<div class="rule-box maths">
  <b>2. Transverse Common Tangent (TCT) Length:</b>
  <div class="formula">L_{TCT} = \sqrt{d^2 - (r_1 + r_2)^2}</div>
  <i>Yahan plus $(r_1 + r_2)$ hota hai kyunki tangent cross karti hai.</i><br>
  <b>PYQ (CHSL 2024 S2 Q53):</b> $r_1 = 7\text{ cm}, r_2 = 5\text{ cm}, d = 15\text{ cm}$.<br>
  $\implies L_{TCT} = \sqrt{15^2 - (7 + 5)^2} = \sqrt{225 - 144} = \sqrt{81} = \mathbf{9\text{ cm}}$.
</div>

<div class="rule-box maths">
  <b>3. Intersecting Chords Property:</b>
  Jab do jeevayein (chords) $AB$ aur $CD$ aapas me circle ke andar ya bahar point $P$ par katti hain:<br>
  <div class="formula">PA \times PB = PC \times PD</div>
  <b>PYQ (CHSL 2020 S1 Q55):</b> Chords $PQ$ aur $RS$ point $L$ par milti hain. $PL = 9, LQ = 4, LS = 6$. Find $RL$.<br>
  $\implies PL \times LQ = RL \times LS \implies 9 \times 4 = RL \times 6 \implies RL = \frac{36}{6} = \mathbf{6\text{ cm}}$.
</div>

<div class="rule-box maths">
  <b>4. Internal Angle Bisector Theorem (त्रिभुज कोण समद्विभाजक):</b>
  $\triangle ABC$ me agar $AD$, $\angle A$ ka bisector hai:<br>
  <div class="formula">\frac{AB}{AC} = \frac{BD}{DC}</div>
  <b>PYQ (CHSL 2021 S1 Q74):</b> $BD = 3.6\text{ cm}, BC = 8\text{ cm} \implies DC = 8 - 3.6 = 4.4\text{ cm}$.<br>
  $\implies \frac{AB}{AC} = \frac{3.6}{4.4} = \frac{9}{11} \implies \mathbf{9 : 11}$.
</div>

<div class="rule-box maths">
  <b>5. Similar Triangles Area & Perimeter Ratio:</b>
  <div class="formula">\frac{\text{Area}(\triangle ABC)}{\text{Area}(\triangle DEF)} = \left(\frac{AB}{DE}\right)^2 = \left(\frac{\text{Perimeter}_1}{\text{Perimeter}_2}\right)^2</div>
  <b>PYQ (CHSL 2024 S1 Q62):</b> Perimeters $24\text{ cm}$ aur $9\text{ cm}$ hain. Area ratio $= \left(\frac{24}{9}\right)^2 = \left(\frac{8}{3}\right)^2 = \mathbf{64 : 9}$.
</div>

<h2>2. Trigonometry Shortcuts & Identities</h2>
<div class="rule-box maths">
  <b>1. Identity Relation: $\sec^2\theta - \tan^2\theta = 1$</b><br>
  $\implies (\sec\theta + \tan\theta)(\sec\theta - \tan\theta) = 1$<br>
  <div class="formula">\text{If } \sec\theta + \tan\theta = k \implies \sec\theta - \tan\theta = \frac{1}{k}</div>
  <b>PYQ (CHSL 2023 S1 Q53):</b> If $\sec\theta + \tan\theta = 5$, then $\sec\theta - \tan\theta = \mathbf{\frac{1}{5}}$. (Instant 2 seconds solve!).
</div>

<div class="rule-box maths">
  <b>2. Minimum Value of Trigonometric Function:</b>
  <div class="formula">\text{Minimum value of } a\csc^2\theta + b\sin^2\theta = 2\sqrt{ab}</div>
  <b>PYQ (CHSL 2020 S1 Q68):</b> Find min value of $8\csc^2\theta + 25\sin^2\theta$.<br>
  $\implies \text{Min} = 2\sqrt{8 \times 25} = 2\sqrt{200} = 2 \times 10\sqrt{2} = \mathbf{20\sqrt{2}}$.
</div>

<div class="rule-box maths">
  <b>3. Componendo & Dividendo (C&D) Trick:</b>
  Agar $\frac{\cos\theta + \sin\theta}{\cos\theta - \sin\theta} = \frac{a}{b}$, toh direct likhein: $\cot\theta = \frac{a + b}{a - b}$ ya $\tan\theta = \frac{a - b}{a + b}$.<br>
  <b>PYQ (CHSL 2020 S1 Q64):</b> $\frac{\cos\theta + \sin\theta}{\cos\theta - \sin\theta} = 8 = \frac{8}{1} \implies \cot\theta = \frac{8 + 1}{8 - 1} = \mathbf{\frac{9}{7}}$.
</div>

<h2>3. Algebra Must-Know Formulas</h2>
<div class="rule-box maths">
  • <b>Master Formula 1:</b> Agar $a + b + c = 0$, tab:
  <div class="formula">a^3 + b^3 + c^3 - 3abc = 0 \iff a^3 + b^3 + c^3 = 3abc</div>
  <i>PYQ (CHSL 2023 S1 Q51):</i> Direct formula pucha gaya tha $\implies \mathbf{0}$.<br>
  • <b>Master Formula 2:</b> $(a+b)^2 - 4ab = (a-b)^2$<br>
  <i>PYQ (CHSL 2022 S3 Q67):</i> $(957 + 932)^2 - 4 \times 957 \times 932 = (957 - 932)^2 = (25)^2 = \mathbf{625}$.<br>
  • <b>Master Formula 3:</b> If $x + \frac{1}{x} = k \implies x^3 + \frac{1}{x^3} = k^3 - 3k$.
</div>

<h2>4. Divisibility Rules (Number System)</h2>
<table>
  <tr><th>Divisor</th><th>Condition / Trick</th><th>PYQ Example</th></tr>
  <tr><td><b>Divisible by 3 / 9</b></td><td>Digit sum 3 ya 9 se kate.</td><td><b>(2024 S1):</b> $627x5$ divisible by 9 $\rightarrow 6+2+7+x+5 = 20+x \implies x=\mathbf{7}$.</td></tr>
  <tr><td><b>Divisible by 11</b></td><td>(Sum of odd places) - (Sum of even places) = 0 ya 11 ka multiple.</td><td><b>(2024 S2):</b> $3c2933k$ divisible by 5 and 11 $\implies k=5, c=1 \implies c+k=\mathbf{6}$.</td></tr>
  <tr><td><b>Divisible by 72 / 24</b></td><td>Co-primes me todein: $72 = 8 \times 9$, $24 = 3 \times 8$. Last 3 digits 8 se katein aur digit sum 9/3 se kate.</td><td><b>(2022 S3):</b> 478656 is divisible by 24 (digit sum = 36).</td></tr>
</table>

<h2>5. Mensuration (2D & 3D Volume / Melting Formulas)</h2>
<div class="rule-box maths">
  • <b>Sphere (गोला):</b> Volume $= \frac{4}{3}\pi r^3$, Surface Area $= 4\pi r^2$.<br>
  • <b>Melting & Recasting Trick:</b> Badha volume = $N \times$ Chota volume.<br>
    <i>PYQ (CHSL 2021 S1 Q70):</i> Sphere $R = 10\text{ cm}$ ko pighla kar $r = 2\text{ cm}$ ke kitne gole banenge?<br>
    $\implies N = \frac{\frac{4}{3}\pi (10)^3}{\frac{4}{3}\pi (2)^3} = \left(\frac{10}{2}\right)^3 = 5^3 = \mathbf{125}$.<br>
  • <b>Cube se Sphere nikalna:</b> Side $a$ ke cube se sabse bada sphere $\implies \text{Radius } r = \frac{a}{2}$.
</div>

<div class="page-break"></div>

<!-- ======================================================== -->
<!-- SECTION 3 : GENERAL INTELLIGENCE & REASONING -->
<!-- ======================================================== -->
<div class="section-banner reasoning">SECTION 3 : GENERAL INTELLIGENCE & REASONING</div>

<h2>1. Open Dice & Closed Dice (1 Question Pakka)</h2>

<div class="diagram-container">
  <!-- Open Dice Representation -->
  <svg width="300" height="150" viewBox="0 0 300 150">
    <g transform="translate(60, 10)">
      <!-- Box 1 -->
      <rect x="50" y="0" width="35" height="35" fill="#faf5ff" stroke="#6b46c1" stroke-width="2"/>
      <text x="67.5" y="22" font-size="14" font-weight="bold" text-anchor="middle">1</text>
      <!-- Box 2, 3, 5 -->
      <rect x="15" y="35" width="35" height="35" fill="#faf5ff" stroke="#6b46c1" stroke-width="2"/>
      <text x="32.5" y="57" font-size="14" font-weight="bold" text-anchor="middle">2</text>
      <rect x="50" y="35" width="35" height="35" fill="#faf5ff" stroke="#6b46c1" stroke-width="2"/>
      <text x="67.5" y="57" font-size="14" font-weight="bold" text-anchor="middle">3</text>
      <rect x="85" y="35" width="35" height="35" fill="#faf5ff" stroke="#6b46c1" stroke-width="2"/>
      <text x="102.5" y="57" font-size="14" font-weight="bold" text-anchor="middle">5</text>
      <!-- Box 6 -->
      <rect x="50" y="70" width="35" height="35" fill="#faf5ff" stroke="#6b46c1" stroke-width="2"/>
      <text x="67.5" y="92" font-size="14" font-weight="bold" text-anchor="middle">6</text>
      <!-- Box 4 -->
      <rect x="50" y="105" width="35" height="35" fill="#faf5ff" stroke="#6b46c1" stroke-width="2"/>
      <text x="67.5" y="127" font-size="14" font-weight="bold" text-anchor="middle">4</text>
    </g>
  </svg>
</div>

<div class="rule-box reasoning">
  <b>Universal Open Dice Rule: Alternate Boxes Opposite Hote Hain</b><br>
  • Ek seedhi line me ek chhodkar agla dabba (alternate face) opposite hota hai.<br>
  • Upar diye figure me:<br>
    - Vertical line me: $1 \leftrightarrow 6$ aur $3 \leftrightarrow 4$<br>
    - Baaki bache do side dabbey: $2 \leftrightarrow 5$<br>
  • <b>Golden Elimination Trick:</b> Jo do number ek dusre ke opposite hote hain, wo kabhi bhi fold hone par <b>saath me (adjacent) nahi dikh sakte</b>. Agar option me saath dikh rahe hain, toh wo option turant galat hai!
</div>

<h2>2. Mathematical Operations (BODMAS Sign Balancing)</h2>
<div class="rule-box reasoning">
  <b>Solving Strategy (Speed Trick):</b><br>
  1. Hamesha <b>Divide ($\div$) sign</b> par pehle nazar daalo. Agar divide sign aisi do sankhyao ke beech aa raha hai jo poori tarah divide nahi hoti (e.g., $14 \div 5$), toh wo option turant eliminate ho jayega (jab tak right side me fraction na ho).<br>
  2. <i>PYQ (CHSL 2022 S3 Q34):</i> $(13 \times 9) \div 3 + (14 \div 7) \times 6 - 21 + (32 \times 2) \implies 39 + 12 - 21 + 64 = \mathbf{94}$.
</div>

<h2>3. Mirror Image & Water Image</h2>
<div class="rule-box reasoning">
  • <b>Mirror on Vertical line (MN / AB at Right or Left):</b> Left becomes Right, and Right becomes Left. (Top aur Bottom same rehte hain).<br>
  • <b>Mirror at Bottom (Water Image):</b> Top becomes Bottom, and Bottom becomes Top. (Left aur Right same rehte hain).<br>
  • <b>Quick Elimination:</b> Sabse pehle pehle aur aakhiri letter ka inversion check karo, 4 me se 2-3 options turant cut jaate hain.
</div>

<h2>4. Set Analogy Patterns (Most Common SSC Logics)</h2>
<table>
  <tr><th>Pattern Type</th><th>Formula Logic</th><th>Real PYQ Example</th></tr>
  <tr><td><b>Sum ka Multiple / Square</b></td><td>$((A + B) / 2)^2 = C$</td><td><b>(2021 S1):</b> $(12, 18, 225) \implies ((12+18)/2)^2 = 15^2 = 225$.</td></tr>
  <tr><td><b>Square Sum Multiple</b></td><td>$(A^2 + C^2) \times 2 = B$</td><td><b>(2020 S1):</b> $(3, 68, 5) \implies (3^2 + 5^2) \times 2 = (9+25) \times 2 = 68$.</td></tr>
  <tr><td><b>Difference of Squares</b></td><td>$A^2 - B^2 = C$</td><td><b>(2021 S1):</b> $(7, 4, 33) \implies 7^2 - 4^2 = 49 - 16 = 33$.</td></tr>
  <tr><td><b>Cube + Square</b></td><td>$A^3 + C^2 = B$</td><td><b>(2022 S3):</b> $(3, 43, 4) \implies 3^3 + 4^2 = 27 + 16 = 43$.</td></tr>
</table>

<div class="page-break"></div>

<!-- ======================================================== -->
<!-- SECTION 4 : GENERAL AWARENESS (GK / GS) -->
<!-- ======================================================== -->
<div class="section-banner gk">SECTION 4 : GENERAL AWARENESS (POLITY, ART & CULTURE, SCIENCE)</div>

<h2>1. Indian Constitution: All Important Articles (100% Guaranteed Topic)</h2>

<table>
  <tr><th>Article No.</th><th>Subject / Provision</th><th>Why It Appears in SSC</th></tr>
  <tr><td><b>Art 14</b></td><td>Equality before law (विधि के समक्ष समता)</td><td>Fundamental Right (Part III)</td></tr>
  <tr><td><b>Art 15</b></td><td>Prohibition of discrimination on grounds of religion, race, caste, sex, birth place</td><td>CHSL 2022 Shift-3 direct question</td></tr>
  <tr><td><b>Art 19</b></td><td>6 Freedoms (Speech, Assembly, Association, Movement, Residence, Profession)</td><td>Most tested fundamental right</td></tr>
  <tr><td><b>Art 20 & 22</b></td><td>Protection against conviction (Art 20) & Protection against arrest/detention (Art 22)</td><td>CHSL 2022 Shift-3 direct question</td></tr>
  <tr><td><b>Art 21 & 21A</b></td><td>Right to Life (Art 21) & Right to Free Elementary Education (Art 21A - 86th Amd)</td><td>Frequently asked in all SSC exams</td></tr>
  <tr><td><b>Art 23 & 24</b></td><td>Right against Exploitation: Human trafficking prohibited (23), Child labour banned (24)</td><td>Very common in Tier-1</td></tr>
  <tr><td><b>Art 32</b></td><td>Constitutional Remedies (Supreme Court Writs) - "Heart and Soul of Constitution"</td><td>Said by Dr. B.R. Ambedkar</td></tr>
  <tr><td><b>Art 40</b></td><td>Organisation of Village Panchayats (DPSP)</td><td>Connects with 73rd Amendment</td></tr>
  <tr><td><b>Art 44</b></td><td>Uniform Civil Code (समान नागरिक संहिता)</td><td>Hot current topic & repeat PYQ</td></tr>
  <tr><td><b>Art 51A</b></td><td><b>Fundamental Duties (मौलिक कर्तव्य)</b>: 10 duties added by 42nd Amd (1976), 11th duty added by 86th Amd (2002)</td><td><b>CHSL 2024 & 2023 dono me aaya!</b></td></tr>
  <tr><td><b>Art 93</b></td><td>Lok Sabha Speaker & Deputy Speaker</td><td>CHSL 2020 Shift-1 direct question</td></tr>
  <tr><td><b>Art 124 to 147</b></td><td>Supreme Court of India (Jurisdiction, Advisory powers under Art 143)</td><td>Canada se li gayi Advisory jurisdiction</td></tr>
  <tr><td><b>Art 243 to 243O</b></td><td>Panchayati Raj (Part IX, 11th Schedule - 29 subjects)</td><td>73rd Amendment 1992 (CHSL 2018)</td></tr>
  <tr><td><b>Art 352, 356, 360</b></td><td>Emergency Provisions (National, President's Rule, Financial)</td><td>Core Polity syllabus</td></tr>
  <tr><td><b>Art 355</b></td><td>Duty of Union to protect States against external aggression & internal disturbance</td><td>CHSL 2023 Shift-1 direct question</td></tr>
  <tr><td><b>Art 368</b></td><td>Power of Parliament to Amend Constitution (Part XX)</td><td>CHSL 2017 & 2021 direct question</td></tr>
</table>

<h2>2. Major Constitutional Amendments & Sources Borrowed</h2>
<div class="rule-box gk">
  • <b>42nd Amendment (1976):</b> Known as "Mini Constitution". Added words <i>Socialist, Secular, Integrity</i> to Preamble. Added Fundamental Duties (Part IVA, Art 51A) on recommendation of Swaran Singh Committee.<br>
  • <b>44th Amendment (1978):</b> Right to Property removed from Fundamental Rights and made legal right (Art 300A).<br>
  • <b>73rd & 74th Amendments (1992):</b> Panchayati Raj (Part IX, 11th Schedule) & Municipalities (Part IXA, 12th Schedule).<br>
  • <b>86th Amendment (2002):</b> Added Article 21A (Right to Education) and 11th Fundamental Duty.<br>
  • <b>Borrowed Features:</b> Canada $\rightarrow$ Advisory jurisdiction of Supreme Court, Quasi-federal structure; USSR $\rightarrow$ Fundamental Duties, Five-Year Plans; UK $\rightarrow$ Parliamentary system, Rule of Law; USA $\rightarrow$ Fundamental Rights, Judicial Review.
</div>

<h2>3. Art & Culture: Classical Dances, Instruments & Masters</h2>
<table>
  <tr><th>Field / Art</th><th>Famous Personality / Legend</th><th>State / Details</th></tr>
  <tr><td><b>Santoor</b></td><td><b>Pandit Shiv Kumar Sharma</b>, Rahul Sharma, Bhajan Sopori, Satish Vyas</td><td>Kashmir traditional instrument (Santoor)</td></tr>
  <tr><td><b>Shehnai</b></td><td><b>Ustad Bismillah Khan</b> (Played at Red Fort on 15 Aug 1947)</td><td>Bharat Ratna awardee</td></tr>
  <tr><td><b>Sitar</b></td><td><b>Pandit Ravi Shankar</b></td><td>Hindustani classical instrumental, Bharat Ratna</td></tr>
  <tr><td><b>Sarangi</b></td><td><b>Ustad Bundu Khan</b>, Ram Narayan</td><td>Classical string instrument (CHSL 2022)</td></tr>
  <tr><td><b>Kathak</b></td><td><b>Birju Maharaj, Shambhu Maharaj</b>, Lachhu Maharaj</td><td>Lucknow Gharana, Uttar Pradesh</td></tr>
  <tr><td><b>Odissi</b></td><td><b>Kelucharan Mohapatra, Sanjukta Panigrahi</b></td><td>Odisha (Kelucharan also played Pakhawaj/Tabla)</td></tr>
  <tr><td><b>Bharatanatyam</b></td><td><b>Rukmini Devi Arundale</b> (1st dancer in Rajya Sabha), Padma Subrahmanyam</td><td>Tamil Nadu</td></tr>
  <tr><td><b>Kuchipudi</b></td><td><b>Mallika Sarabhai</b>, Yamini Krishnamurthy</td><td>Andhra Pradesh</td></tr>
  <tr><td><b>Folk Paintings</b></td><td><b>Warli</b> (Maharashtra), Madhubani (Bihar), Pattachitra (Odisha)</td><td>Tribal / Folk art forms</td></tr>
  <tr><td><b>State Festivals</b></td><td><b>Saga Dawa, Losoong, Sakewa</b> (Sikkim); <b>Nehru Boat Race</b> (Kerala)</td><td>North-East & South India festivals</td></tr>
</table>

<h2>4. Geography & Environment Essentials</h2>
<div class="rule-box gk">
  • <b>Tropic of Cancer (23°30' N):</b> Passes through 8 Indian States $\rightarrow$ <b>Gujarat, Rajasthan, MP, Chhattisgarh, Jharkhand, West Bengal, Tripura, Mizoram</b> (Mnemonic: <i>G-R-M-C-J-W-T-M</i>).<br>
  • <b>Atmosphere Layers:</b><br>
    1. <i>Troposphere (क्षोभमंडल):</i> Lowest layer, avg height 13 km, all weather events occur here.<br>
    2. <i>Stratosphere (समतापमंडल):</i> Contains Ozone ($O_3$) layer, ideal for flying airplanes.<br>
    3. <i>Mesosphere (मध्यमंडल):</i> Coldest layer (up to 80 km), meteorites (उल्कापिंड) burn here.<br>
    4. <i>Thermosphere / Ionosphere:</i> Radio transmission.<br>
  • <b>Major Sea Ports:</b> West Coast: <i>Kandla/Deendayal (Gujarat), Mormugao (Goa), JNPT (Maharashtra)</i>. East Coast: <i>Haldia (WB), Paradip (Odisha), Tuticorin/V.O. Chidambaranar (TN)</i>.
</div>

<h2>5. General Science & Computer Basics</h2>
<table>
  <tr><th>Topic</th><th>Core Key Points / Formulas</th><th>PYQ Reference</th></tr>
  <tr><td><b>Human Body</b></td><td>• <b>Stomach Acid:</b> HCl (pH 1.5 - 3.0) activates Pepsin for protein digestion.<br>• <b>Insulin:</b> Secreted by Pancreas beta cells; deficiency causes Diabetes Mellitus.<br>• <b>ANF (Atrial Natriuretic Factor):</b> Secreted by Heart atrial wall to regulate blood pressure.</td><td>CHSL 2022 S3<br>CHSL 2020 S1</td></tr>
  <tr><td><b>Chemistry</b></td><td>• <b>Galvanisation:</b> Applying a zinc coating to iron/steel to prevent rusting.<br>• <b>Law of Constant Proportion:</b> Joseph Proust (1806).<br>• <b>Bakelite:</b> Thermosetting plastic, poor conductor of heat and electricity.</td><td>CHSL 2020 S1<br>CHSL 2022 S3<br>CHSL 2017 S1</td></tr>
  <tr><td><b>Physics</b></td><td>• $KE = \frac{1}{2}mv^2$ | $1\text{ Joule} = 1\text{ N}\cdot\text{m}$.<br>• Electric Field: First introduced by Michael Faraday.<br>• Refractive Index: $n = \frac{c}{v}$ ($c = \text{speed of light in vacuum}$).</td><td>CHSL 2018 S1<br>CHSL 2021 S1<br>CHSL 2022 S3</td></tr>
  <tr><td><b>Computer Basics</b></td><td>• <b>MS Excel 365:</b> Close workbook shortcut $\rightarrow$ <b>Ctrl + W</b>.<br>• Excel grid: Rows are identified by <b>Numbers</b> ($1, 2, 3\dots$), Columns by <b>Letters</b> ($A, B, C\dots$).<br>• <b>Average Function:</b> Present under the <b>AutoSum</b> drop-down menu.<br>• <b>ASCII:</b> American Standard Code for Information Interchange (128 characters).</td><td>CHSL 2023 S1<br>CHSL 2024 S2<br>CHSL 2024 S1<br>CHSL 2021 S1</td></tr>
</table>

</body>
</html>
"""

html_path = os.path.abspath("master_notes.html")
pdf_path = os.path.abspath("SSC_CHSL_Master_Revision_Notes.pdf")

with open(html_path, "w", encoding="utf-8") as f:
    f.write(html_content)

print("HTML written successfully. Converting to PDF via Edge headless...")

edge_path = r"C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe"
if not os.path.exists(edge_path):
    edge_path = r"C:\Program Files\Microsoft\Edge\Application\msedge.exe"

cmd = [
    edge_path,
    "--headless=new",
    "--disable-gpu",
    f"--print-to-pdf={pdf_path}",
    html_path
]

res = subprocess.run(cmd, capture_output=True, text=True)
if os.path.exists(pdf_path):
    print(f"SUCCESS: PDF created at {pdf_path} (Size: {os.path.getsize(pdf_path)} bytes)")
else:
    print(f"FAILED: {res.stderr}")
