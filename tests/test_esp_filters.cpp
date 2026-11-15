#include "../app/src/main/jni/WOLTAJ/Main/ESP/ESPFilterLogic.h"
#include <cassert>
#include <iostream>

int main() {
    for (bool option : {false, true}) {
        for (bool isEnsure : {false, true}) {
            for (bool isAI : {false, true}) {
                assert(ESPFilterLogic::ShouldHideBot(option, isEnsure, isAI)
                       == (option && (isEnsure || isAI)));
                assert(ESPFilterLogic::ShouldShowNation(option, isEnsure, isAI)
                       == (option && !isEnsure && !isAI));
            }
        }
    }
    assert(!ESPFilterLogic::ShouldHideBot(false, false, true));
    assert(ESPFilterLogic::ShouldHideBot(true, false, true));
    std::cout << "All 16 filter cases passed\n";
}
