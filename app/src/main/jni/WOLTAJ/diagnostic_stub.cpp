// Diagnostic-only no-op replacement for the obsolete anti-cheat hook module.
// This file allows build-system diagnostics of the native ImGui sources.
// It does NOT update PUBG compatibility or provide any in-game capability.
#include <cstddef>

void *anogs_thread(void *) {
    return nullptr;
}
