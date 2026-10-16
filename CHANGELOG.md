# Changelog

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
