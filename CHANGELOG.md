# Changelog

## [0.3.0](https://github.com/adwiarifin/gem-match/compare/v0.2.0...v0.3.0) (2026-10-20)


### Features

* **app:** add a stats screen with lifetime records and the last 100 runs ([b8dc7a9](https://github.com/adwiarifin/gem-match/commit/b8dc7a994c3fb86391bff4e12872c3e36b0df57a))
* **app:** record each finished run once in runs.txt and lifetime.txt ([f92ac06](https://github.com/adwiarifin/gem-match/commit/f92ac06bbb17a1319de4af0630d0d1982e71c910))
* **app:** show a game-over card with the run's stats and a Play again button ([beae98e](https://github.com/adwiarifin/gem-match/commit/beae98ee7d923c16986473bba5d2e09914796320))
* **app:** time a run in the foreground and save its clocks with the game ([d84607e](https://github.com/adwiarifin/gem-match/commit/d84607eda5ad1b859eeede4e405a2eeca27010c3))
* **core:** end the run when no move is left; the reshuffle and its tests go because game over replaces it (SPEC assumption 7) ([9c6e62e](https://github.com/adwiarifin/gem-match/commit/9c6e62e00eef9c691a9cc8c5abc498b49fdcb12e))
* **core:** finish a run into a record and fold records into lifetime totals ([16b089a](https://github.com/adwiarifin/gem-match/commit/16b089a1924ccd7e9bd125585de3edbe2bb364a2))
* **core:** refill avoids matching a neighbour so dead boards come sooner ([c9287b6](https://github.com/adwiarifin/gem-match/commit/c9287b665fdf545acf636545fdab9e261390fb40))
* **core:** save a run's stats and clocks in GM3; three retyped assertions now expect GM3 instead of GM2 ([c9d357d](https://github.com/adwiarifin/gem-match/commit/c9d357d84cf59b3434d0c88e196b085311822d53))
* **core:** track a run's stats from the steps of each move ([8d29316](https://github.com/adwiarifin/gem-match/commit/8d293167b60c290763935d6c4d6661c5259dc252))


### Tests

* **core:** measure how long a run lasts before the board is dead ([8f69b19](https://github.com/adwiarifin/gem-match/commit/8f69b19b24d2ec041c4cd918dc23eb62be58c721))
* **core:** pin a near-dead save and add the phone runbook for game over ([28cc044](https://github.com/adwiarifin/gem-match/commit/28cc044405c4bce3e6a9b22ea9bb5257d5f08794))


### Documentation

* **decisions:** record harsher spawn as D23 ([32e5dcb](https://github.com/adwiarifin/gem-match/commit/32e5dcb2b705185acf6286b7989ddbab24fcdf1b))
* **decisions:** record measured run length as D22 ([a582c91](https://github.com/adwiarifin/gem-match/commit/a582c91707a0ff7144cdd7c324bf5d20ee0b0e18))
* describe game over and run stats, record D24 and the emulator run ([88656c5](https://github.com/adwiarifin/gem-match/commit/88656c587411ae5b47cb9c6c78042fa64e5cbf09))
* **intent:** add game over and run stats intent ([4cf846e](https://github.com/adwiarifin/gem-match/commit/4cf846e1a6c130eb6e2745788a2cb1df4c24bc57))
* record the A52 run of the game-over runbook ([49cf737](https://github.com/adwiarifin/gem-match/commit/49cf7378fa5eb0805259eb6cb9fc551f1743d5c3))
* **spec:** a dead board loads as an over game ([cd832e1](https://github.com/adwiarifin/gem-match/commit/cd832e1328e749220454bb99187eef39882ed10a))
* **spec:** add game over and run stats ([643b084](https://github.com/adwiarifin/gem-match/commit/643b0849097f8f25c8af008958886a6c1126176a))
* **spec:** add harsher spawn and task T37b ([28ec38f](https://github.com/adwiarifin/gem-match/commit/28ec38fa699ea45d168ce7297526f576411168e0))
* **tasks:** note the measured values the owner still has to record ([bdf683e](https://github.com/adwiarifin/gem-match/commit/bdf683e39a089773fb256a9231314111ed98d4e1))
* **tasks:** plan game over and run stats ([202cf46](https://github.com/adwiarifin/gem-match/commit/202cf46726984b0a0f59d9ce278d028bf9d42a04))
* **tasks:** tick T39 to T41 and Checkpoint K ([c415261](https://github.com/adwiarifin/gem-match/commit/c4152616edbdb0ad60238c32330201f36bb464c2))
* **tasks:** tick T42 to T44 ([482b1ae](https://github.com/adwiarifin/gem-match/commit/482b1ae90a221ad70114eaaa73e938b6fcdee7be))
* **tasks:** tick T45 and note the device looks wait for T46 ([391c9ba](https://github.com/adwiarifin/gem-match/commit/391c9baeb69ac48b020bc60d68411c5458be412b))
* **tasks:** tick T46 ([6b83722](https://github.com/adwiarifin/gem-match/commit/6b8372226481094ea55e739f58298b453044605c))
* **tasks:** tick the signed 0.2.0 install over the old build on the A52 ([15812d5](https://github.com/adwiarifin/gem-match/commit/15812d5447ca2becd17971d0aa5cb2be7ab0db45))


### Style

* wrap the lines and nesting detekt flagged in earlier tasks ([70f5d7e](https://github.com/adwiarifin/gem-match/commit/70f5d7e0febb4f1a97cb8757741131d465a364fe))

## [0.2.0](https://github.com/adwiarifin/gem-match/compare/v0.1.2...v0.2.0) (2026-10-17)


### Features

* **app:** effect animations for blast, line, cross and colour gems ([b820f96](https://github.com/adwiarifin/gem-match/commit/b820f960685988278b4c32806ebeb04e90d68f06))
* **app:** idle hint prefers a match and falls back to a special swap ([affa525](https://github.com/adwiarifin/gem-match/commit/affa525a06ec21ec0f691f29e270d9f0b53df18a))
* **app:** overlapping chain effects, tap to skip, converted gems shown, ringed full X ([5ed1c36](https://github.com/adwiarifin/gem-match/commit/5ed1c362731926fe7896acb42cdbe6ec3677437e))
* **app:** special gem overlays and shimmer ([58c5227](https://github.com/adwiarifin/gem-match/commit/58c52275709f3548fe28084790e79b1ccd2e4446))
* **app:** white gem becomes a pentagon ([491bc21](https://github.com/adwiarifin/gem-match/commit/491bc213a7d383cb939e08639c412d4224774fa8))
* **core:** a colour gem covered by an effect takes the colour of the gem that covered it ([66f1c73](https://github.com/adwiarifin/gem-match/commit/66f1c7349ab3333413dd2ebddc10de9b400566f1))
* **core:** add Piece and Special, Board stores Piece ([74f6e11](https://github.com/adwiarifin/gem-match/commit/74f6e11bf03693b8f577f265d9e60ea36b25ff09))
* **core:** blast, line and cross fire on swap with Step.Activate ([b2df047](https://github.com/adwiarifin/gem-match/commit/b2df04747127983cbc890cd863b27c3152ebf0c0))
* **core:** chain reactions fire each covered special once ([6c3484d](https://github.com/adwiarifin/gem-match/commit/6c3484d6de446877298b5c39535e9405cdfd09e5))
* **core:** classify match groups into special gems ([b458950](https://github.com/adwiarifin/gem-match/commit/b45895054f13ae53baa3b6fddee6b230b5ad0dcf))
* **core:** colour gem clears its partner's colour ([f9ad212](https://github.com/adwiarifin/gem-match/commit/f9ad212571353738ca156eb5d766e96034e25e34))
* **core:** combos for colour gems and paired specials ([4591cc5](https://github.com/adwiarifin/gem-match/commit/4591cc597c339d7d1089679131d58f8fbdd388f4))
* **core:** player's match creates special pieces in resolveSwap ([63039b1](https://github.com/adwiarifin/gem-match/commit/63039b1daa88649256f726a6036f181cf8f63175))
* **core:** save format GM2 with special pieces, GM1 still readable ([6da7579](https://github.com/adwiarifin/gem-match/commit/6da75790199f4f71ac10f7ae55a901b1ca479f93))
* **core:** short L, short T, L cross and long T shape rules ([b7a336a](https://github.com/adwiarifin/gem-match/commit/b7a336a94c2bc86bcd5ed07c985a65ede12a1a7e))
* **core:** short T and long T tiers, crossing shapes beat the colour gem, short T of four stays plain ([090b194](https://github.com/adwiarifin/gem-match/commit/090b1940fd624af98e980af5eef30b20a809b1f7))
* **core:** specials are inert in matches, special swaps always legal, add findMatchMoves ([54a1b51](https://github.com/adwiarifin/gem-match/commit/54a1b5110e7906acef6b84e97cb277c681c32c1e))
* **core:** specials join matches and fire, and matches make specials at any cascade depth ([57312e2](https://github.com/adwiarifin/gem-match/commit/57312e2d77aa91902853aa31c2ef2b584bb16d2c))
* short X and full X special kinds with effects, overlays and save letters ([77fe101](https://github.com/adwiarifin/gem-match/commit/77fe10196cdda7d010cd39e946a62032034f81c8))
* two of the same line or blast make one bigger effect, three lines or a 5x5 ([20a9772](https://github.com/adwiarifin/gem-match/commit/20a977274955dfc84e303dae1162857de71d84b1))


### Bug Fixes

* **app:** a tap skips only an accepted move, not a spring-back or a rejected swap ([6d70deb](https://github.com/adwiarifin/gem-match/commit/6d70debcef3e06b9e5fe11dc32e189d9723924ae))
* **app:** dark mark on white specials, wrap shimmer clock, seamless colour wheel ([5c7da5f](https://github.com/adwiarifin/gem-match/commit/5c7da5ff7dc3d661b2c37bc65f95d9646ac93c51))
* **app:** sheen colour set before drawing, precompute effect reach, no per-frame pairs ([a276e3a](https://github.com/adwiarifin/gem-match/commit/a276e3a0138f230b0fc8e74e6e3027d3c73a6404))
* **core:** a fire in a clear no longer stops its matches from making specials ([0b7564e](https://github.com/adwiarifin/gem-match/commit/0b7564edbb19fa4d4184846f9e7b93e9239cec46))
* **core:** effectCells fails loudly for the colour gem instead of returning no cells ([90a40a5](https://github.com/adwiarifin/gem-match/commit/90a40a5936d0f261883282fbf72005ed713b3eae))
* **scripts:** floor guard accepts retyped assertions, still flags deleted ones ([4138b96](https://github.com/adwiarifin/gem-match/commit/4138b96d077a5fb806c199b8a3339cf49b16e2a3))
* **scripts:** floor guard keeps only whitelisted wrappers, reads through -diff attributes, and a test-count floor catches tests that stop running ([c3b8d42](https://github.com/adwiarifin/gem-match/commit/c3b8d42e11531e4eadd55811bccc91b099dc93e0))
* **scripts:** floor guard needs a kept argument to accept a retyped assertion, with its own self-test ([1beb81c](https://github.com/adwiarifin/gem-match/commit/1beb81c8cf4aaa24fdf61996776c00ed285b3ab4))
* **scripts:** floor guard rejects tautologies, neutered and commented-out assertions, multi-line swaps, moved tests and removed test markers ([33d8735](https://github.com/adwiarifin/gem-match/commit/33d873528014558efbfcd355adf1babfebcad34d))


### Performance

* **app:** draw each flashing cell once, no per-frame iterators, shimmer computed once per frame ([af6319c](https://github.com/adwiarifin/gem-match/commit/af6319ca6f307e070af99b89f701c1189cedcdd2))


### Refactoring

* Board.get returns Piece, steps and sprites carry Piece ([b9e62d7](https://github.com/adwiarifin/gem-match/commit/b9e62d738d1d546053be7051417aec636d9659b8))
* **core:** ALL_CELLS lives with the board ([ffdd246](https://github.com/adwiarifin/gem-match/commit/ffdd246968c3cb78a2fab2b0eeb56bfd327f0c50))
* **core:** one function works out a special's cells for the swap and for a chain ([d2d20f8](https://github.com/adwiarifin/gem-match/commit/d2d20f8daee0267ed0d7f393a4ea14699d1351be))
* **core:** pairCells takes a Special, not a nullable one ([c5ea125](https://github.com/adwiarifin/gem-match/commit/c5ea125112976a7828b573a40e36a9dec9e795b7))
* **core:** resolveSwap tests the first depth once and no longer shadows a parameter ([5fc1092](https://github.com/adwiarifin/gem-match/commit/5fc109267eacbd9a8d23d5bd772ba91c680781c7))
* **core:** share ALL_CELLS and satisfy detekt line limits ([19663f9](https://github.com/adwiarifin/gem-match/commit/19663f94aceec2596ab455af4e3cc6279f2b4126))
* **core:** the save codec names its gem and special lookups ([27da8c6](https://github.com/adwiarifin/gem-match/commit/27da8c6a2c9f8716d6ef2a57dc94440993f9ddd9))


### Tests

* **app:** every special kind plays to the core board, skips and chain timing pinned ([2abeabb](https://github.com/adwiarifin/gem-match/commit/2abeabbf900e016b7a372cc6427a65e04930deac))
* **app:** special pieces play back, fall as units and survive resume ([bb44449](https://github.com/adwiarifin/gem-match/commit/bb4444987bf1c7d0759226bfec8ab74f6e3f4a39))
* **app:** tap-skip policy and doubled-line beam sets pinned, subList in forSteps ([0728f7e](https://github.com/adwiarifin/gem-match/commit/0728f7ec159496ca72ef0c8fc46f2cb1d692a0fe))
* cascade creation position, fire-once, hanging tie-break, reshuffle with specials, session with a special ([3721c5f](https://github.com/adwiarifin/gem-match/commit/3721c5f1d594aa9caf43212bbeb8fdce1df0afea))
* **core:** every special kind plays a whole game, and a board of only specials settles ([734ae16](https://github.com/adwiarifin/gem-match/commit/734ae16ddc0ca22916ea8bb60765b960e7a756ce))
* **core:** keep the assertion lines short without changing their messages ([9623e3d](https://github.com/adwiarifin/gem-match/commit/9623e3d123c2b9f61a61099f92e3df585ffedc55))
* **core:** keep the cascade position test's lines short ([ca9bd13](https://github.com/adwiarifin/gem-match/commit/ca9bd133d48c59235e9ab4999811375cfac46721))
* **core:** pin cells cleared by every pair of special gems ([7af9459](https://github.com/adwiarifin/gem-match/commit/7af94594a5c0a976e68a3a53b222eb843538fe7f))
* **core:** pin special gem scoring with literal scores ([568f1d9](https://github.com/adwiarifin/gem-match/commit/568f1d9526ed4de3d76ff9f85452ed719b681650))
* **core:** pin the swap fire order, the colour gem's covering colour and the converted order; clearer effectCells doc ([e0d97dd](https://github.com/adwiarifin/gem-match/commit/e0d97dda7a052057a423b5fd9b43d3920377da02))
* **core:** soak checks reshuffles happen and uses an independent chooser ([2345245](https://github.com/adwiarifin/gem-match/commit/2345245772ba1761e601665c45256c30b06e64a1))
* **core:** soak games with special gems over many seeds ([411eb6c](https://github.com/adwiarifin/gem-match/commit/411eb6cdd9fb63dada4a6db743da47750d3fe928))
* pin the GM1 upgrade with a real v0.1.2 save, GM2 limits and corrupt files ([c423791](https://github.com/adwiarifin/gem-match/commit/c4237916eb9213ed67a518ffe3eb479eae0d66b4))


### Documentation

* a fire no longer stops creation (D21) ([b7c3f29](https://github.com/adwiarifin/gem-match/commit/b7c3f296ed2e061bbb117808608d0d689699236e))
* add release pipeline blog ideas ([120b1ee](https://github.com/adwiarifin/gem-match/commit/120b1eefb4a1ac4cdd1d0288e2b63c2f122024d6))
* add special gems intent and amend spec ([2fb47e0](https://github.com/adwiarifin/gem-match/commit/2fb47e0e99f848c48f1680279d9bbe7e37b163cd))
* audit the unchecked boxes in todo and plan against what exists ([ddc7dbd](https://github.com/adwiarifin/gem-match/commit/ddc7dbd72755188aaea64d97957731d320ac6b36))
* close out special gems, spec status and README, tick T36 and Checkpoint I ([f2f56aa](https://github.com/adwiarifin/gem-match/commit/f2f56aabd83a6b4ff5e480234b3b61d3b9ea8807))
* close special gem questions, split board refactor and renumber tasks ([8b218ad](https://github.com/adwiarifin/gem-match/commit/8b218ad25c30f0b0f6aadb60ef0928447b2531b3))
* colour gem covered by an effect takes the covering colour (D19) ([cdd4246](https://github.com/adwiarifin/gem-match/commit/cdd42464131475f88a60fb0e8181ea9be7bf130f))
* mark stale plan lines superseded, name the guard self-test in CONSTRAINTS ([1c48466](https://github.com/adwiarifin/gem-match/commit/1c48466ef070dcebceede2326ab951dcc21c3785))
* name the test-count floor and say the floor guard is a local and release-time gate ([af7c389](https://github.com/adwiarifin/gem-match/commit/af7c3899fc90f1e7b01cb1d0c8cc8da571d8916d))
* plan special gems and record their decisions ([8afa639](https://github.com/adwiarifin/gem-match/commit/8afa63905b8f059d9897fa6bbd37c3b6380779dd))
* record the measured sizes, coverage and A52 frame time with special gems ([89e7560](https://github.com/adwiarifin/gem-match/commit/89e7560fb9b956d62d20cad65e32bae4425d1559))
* settle special gem step order and conversion in spec and plan ([5f8a720](https://github.com/adwiarifin/gem-match/commit/5f8a7207d54627c7d94c73a99f49143d1c96bbc3))
* special gem rules page, spec and plan revised, D16 and D17 ([5e349fd](https://github.com/adwiarifin/gem-match/commit/5e349fde2df1bd699af2525c49dff98b03da7e2e))
* T tiers (D18), review fixes in the rules page, spec, decisions and plan ([ba8448c](https://github.com/adwiarifin/gem-match/commit/ba8448ca99d5e3c4f65f30a33ea3f5956642b453))
* tick T19 to T23 and T29, note T29 done early ([3a6ae29](https://github.com/adwiarifin/gem-match/commit/3a6ae29e77e07f7d2dfd99dfda41178752710755))
* tick T20 and Checkpoint E that were left open ([a8545f4](https://github.com/adwiarifin/gem-match/commit/a8545f4d5056c4d869364c3d416043348611fe77))
* tick T24 to T27 and checkpoint F ([0cef0a1](https://github.com/adwiarifin/gem-match/commit/0cef0a12e9223ec0dcfe6246bf6be5a00b959523))
* tick T28 and T30 ([eae9b75](https://github.com/adwiarifin/gem-match/commit/eae9b75c022d23abdd48974d12494787e4573eff))
* tick T31 to T33 and checkpoint G ([68d3a05](https://github.com/adwiarifin/gem-match/commit/68d3a056b3fe7dd36a1f7a9ad5bba595cabe778b))
* tick T34 and T35 ([188d255](https://github.com/adwiarifin/gem-match/commit/188d255b6aeb6fbe8122c1ebcb90091b1250fe5a))
* two of the same line or blast make one bigger effect (D20) ([6114fce](https://github.com/adwiarifin/gem-match/commit/6114fced077c04fa89f10d5f6c5f84015ac675de))

## [0.1.2](https://github.com/adwiarifin/gem-match/compare/v0.1.1...v0.1.2) (2026-10-16)


### Bug Fixes

* **ci:** match the signer certificate line on any apksigner format ([499f3a4](https://github.com/adwiarifin/gem-match/commit/499f3a425d26ef2d7e90def73de51794b001eb59))

## [0.1.1](https://github.com/adwiarifin/gem-match/compare/v0.1.0...v0.1.1) (2026-10-16)


### Bug Fixes

* **ci:** read the signer certificate on newer apksigner ([cb131df](https://github.com/adwiarifin/gem-match/commit/cb131df909faed08da87fae41e237815f1167810))


### Build

* verify the aapt2 artifacts ([56f3cab](https://github.com/adwiarifin/gem-match/commit/56f3cab2fd122860b1db910acd4adbb3edaec84c))


### CI

* release on every conventional commit type except chore ([e3c5e13](https://github.com/adwiarifin/gem-match/commit/e3c5e13b7d828410512c1ae27eed4f647a413347))


### Documentation

* describe the release triggers ([8aa8e9a](https://github.com/adwiarifin/gem-match/commit/8aa8e9ad14ff8c9c0cfaf881c6d62ae2e7187ef7))
* drop the release-as note after the first release ([bbb5905](https://github.com/adwiarifin/gem-match/commit/bbb59056e94e33496b0c3dcd35c1e83a653af4d5))

## 0.1.0 (2026-10-16)


### Features

* adaptive launcher icon ([24b22dc](https://github.com/adwiarifin/gem-match/commit/24b22dc2aeef8685a64d40e35a0dd4162a02475f))
* **app:** act on release and add reshuffle and new game transitions ([f9d00d3](https://github.com/adwiarifin/gem-match/commit/f9d00d31243bae017055a7dafc017a93f6966146))
* **app:** add empty surfaceview app module ([3f4401b](https://github.com/adwiarifin/gem-match/commit/3f4401b71f0e66bf895ecd3a759b22ae077f7b1a))
* **app:** add hud, canvas new game dialog and reshuffle animation ([f79bf4e](https://github.com/adwiarifin/gem-match/commit/f79bf4e912d2187fb931adbf1b34ebd91fc55fff))
* **app:** add swipe input with swap and invalid-move bounce animation ([f4b1003](https://github.com/adwiarifin/gem-match/commit/f4b100385a1eba9df6d439544eca9f7a7ad95f21))
* **app:** animate clear, fall and spawn steps including cascades ([81b1979](https://github.com/adwiarifin/gem-match/commit/81b197917f328c5d874e1a9a824d307bf41463a1))
* **app:** autosave game and persist best score with atomic writes ([807860f](https://github.com/adwiarifin/gem-match/commit/807860fac2ef17a497c3f53700770d43a787e045))
* **app:** draw board from core on a fixed-timestep loop ([837a18f](https://github.com/adwiarifin/gem-match/commit/837a18ff3086c9368aea658ede88d7ebbbc972f6))
* **app:** draw gems as distinct vector shapes ([014b95f](https://github.com/adwiarifin/gem-match/commit/014b95fa0e5640d9ea740a11be08f4cad866d5d4))
* **app:** pick up and drag gems with live swap preview ([556837a](https://github.com/adwiarifin/gem-match/commit/556837a4ab86d99d37f1e866c3544e55563a6bcf))
* **app:** pulse a valid move after 8 seconds idle ([0ca2bbc](https://github.com/adwiarifin/gem-match/commit/0ca2bbc417558c2052956232e18f9900d4bdb0e3))
* **core:** add board, positions, gems and seeded rng ([178590e](https://github.com/adwiarifin/gem-match/commit/178590e283551eb88bd0944b9ffcf27620283f61))
* **core:** add cascade resolution and tiered scoring ([f7b8874](https://github.com/adwiarifin/gem-match/commit/f7b887438ec02e845849d8e96b4f3da32b095c8d))
* **core:** add drag preview and raise swipe commit threshold to 0.35 ([dd3dbec](https://github.com/adwiarifin/gem-match/commit/dd3dbeca7853052897a24cee421bc30d9f229955))
* **core:** add game state, new game and reshuffle ([39fad98](https://github.com/adwiarifin/gem-match/commit/39fad98f1e06e8b39ff66d6b7a3634877bf603a7))
* **core:** add match detection and match groups ([0d2d32b](https://github.com/adwiarifin/gem-match/commit/0d2d32b60cef594a379b46bc725687481ae5924e))
* **core:** add swap validation and valid move search ([6674aaa](https://github.com/adwiarifin/gem-match/commit/6674aaac9354820ee9ff2437c0efe90e803088f4))
* **core:** add swipe to move geometry ([b1cc443](https://github.com/adwiarifin/gem-match/commit/b1cc44348fe61f10863dc388af351da43368bbf5))
* **core:** add versioned save codec with validation ([fb9a773](https://github.com/adwiarifin/gem-match/commit/fb9a77393e2e53dc283abe730696cfd3e6eb520f))


### Bug Fixes

* cap save file size and stop the score wrapping negative ([30f20bf](https://github.com/adwiarifin/gem-match/commit/30f20bf1d68733bf71ffd484e8e008cf75f294de))
* credit an abandoned move's score to Best when New Game starts mid-animation ([d0d89e0](https://github.com/adwiarifin/gem-match/commit/d0d89e075ad003c5e9ef0e35eb3868a19c17975a))
* drop a leftover reshuffle notice whenever the scene is reloaded ([f7890a9](https://github.com/adwiarifin/gem-match/commit/f7890a9b80c720c562870a1a14ef7f27602c0482))
* keep the held gem lifted when it was part of the pulsing hint ([6ddc789](https://github.com/adwiarifin/gem-match/commit/6ddc789ff1fa4f5a581bd562aaa3bf4928cf14ab))
* launch the activity as singleTask so another app cannot start a second instance ([cd4c12f](https://github.com/adwiarifin/gem-match/commit/cd4c12fc2cd76241776cf9945ccf42428aa608a1))
* let the pause flush read the newest state when its write runs ([f3e1f90](https://github.com/adwiarifin/gem-match/commit/f3e1f90ecd86a5195779ca12023f5aa63169ec60))
* put a held gem down when a new touch starts without a finger-up ([3602910](https://github.com/adwiarifin/gem-match/commit/3602910684ef26bfdd1fed0084b93964d9f8be30))
