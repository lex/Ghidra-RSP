# Ghidra-RSP

A Nintendo 64 RSP processor module and loader for [Ghidra](https://github.com/NationalSecurityAgency/ghidra).

![Demo](https://github.com/Random06457/Ghidra-RSP/assets/28494085/64c587df-c587-4ef0-8982-8126af713f57)

# Usage
- Drop a RSP binary in Ghidra (content of IMEM).

- In the loader options, you can decide whether to load the RSP binary as a boot microcode or main microcode. (This will just change the base address of the binary, 0x1000 or 0x1080 respectively for boot or main microcode).

- If the `DMEM File` option is left empty, after hitting the OK button the loader will prompt for a path to load the content of DMEM from. (You can choose not to import any DMEM content by clicking the cancel button in the file prompt).

## Headless

The loader works with `analyzeHeadless`. There is no file prompt in headless mode, so pass DMEM with `-loader-dmemFile` (omit it to import without DMEM content):

```
analyzeHeadless <project_dir> <project_name> -import ucode.bin -loader GhidraRSPLoader \
    -loader-dmemFile ucode_data.bin -loader-isBoot false
```

# Installation

- Open Ghidra
- File-> Install Extensions
- Import zip file from the release section

# Compatibility

Supports Ghidra 12.0 and newer (requires Java 21). Ghidra 12 changed the loader API, so builds for Ghidra 10.x/11.x are only available from older releases.

# Building

```
$GHIDRA_INSTALL_DIR/support/gradle/gradlew -PGHIDRA_INSTALL_DIR=$GHIDRA_INSTALL_DIR
```

The extension zip is written to `dist/`.
