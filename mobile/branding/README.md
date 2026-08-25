# Launcher icon assets

`d4wid-ring-icon-master.png` is the approved full-color 1:1 source. It keeps the supplied Challenger/Bluetooth composition and uses the app palette for the two independently controlled halos:

- left halo: cyan `#00E5E5`;
- right halo: orange `#FF6A00`.

Regenerate the Android density variants and the transparent monochrome themed-icon layer with the bundled workspace Python runtime:

```bash
/Users/karolsojka/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/bin/python3 \
  mobile/branding/generate_launcher_assets.py
```

Do not resize or recompress the generated files manually; update the master and rerun the script.
