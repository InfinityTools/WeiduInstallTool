# Changelog

## [1.0.0] - 2023-11-04

- *First stable release*
- Added Russian translation (by abalabokhin)
- Fixed typos and cosmetic issues

## [0.10.3] - 2023-10-27

- Added option to create debug files of mod installations in a user-defined folder instead of the game directory.
- Added option to auto-apply performance or diagnostic parameters to mod installations:
  - Performance: `--quick-log`
  - Performance: `--safe-exit`
  - Diagnostic: `--print-backtrace`
  - Diagnostic: `--debug-ocaml`
  - Diagnostic: `--debug-boiic`
  - Diagnostic: `--debug-change`
  - Apply user-defined set of parameters
- Fixed issues with mixed-case paths on Linux which resulted in failures to retrieve mod components info.
- Improved parsing of Project Infinity metadata:
  - added support for C-style comments (`//`) and Windows-style comments (`;`) 

## [0.10.2] - 2023-10-22

- Added Brazilian Portuguese translation (by Felipe).
- Added warning dialog if a mod is installed from a path outside of the game directory.
- Reduced likelihood of connection failure for the WeiDU binary download operation.
- Improved Ini parser to properly handle malformed Project Infinity metadata:
  - Correctly handle multiline mod descriptions
  - Fix erroneously detected comment prefixes inside BEFORE/AFTER definitions

## [0.10.1] - 2023-10-19

- *First public release candidate*
- Added French translation (by JohnBob)
- Added German translation (by Argent77, proofreading by Shai Hulud)
