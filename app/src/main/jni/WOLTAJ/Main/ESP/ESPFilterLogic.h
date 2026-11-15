#pragma once

// Pure ESP filtering rules, independent from the game SDK.
// This does not provide offsets or claim compatibility with a PUBG release.
namespace ESPFilterLogic {
constexpr bool ShouldHideBot(bool hideBots, bool isEnsure, bool isAI) {
    return hideBots && (isEnsure || isAI);
}

constexpr bool ShouldShowNation(bool showNation, bool isEnsure, bool isAI) {
    return showNation && !isEnsure && !isAI;
}
} // namespace ESPFilterLogic
