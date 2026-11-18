// NOVA standalone preview for Dear ImGui (same ImGui version as the original app).
// This is a UI/render/input test only: no hooking, no game memory, no anti-cheat modifications.
#include "imgui.h"
#include "imgui_impl_opengl3.h"
#include <jni.h>
#include <GLES3/gl3.h>
#include <chrono>
#include <cmath>
#include <algorithm>
#include <atomic>

namespace {
bool ready = false;
std::atomic<int> overlayFlags{16};
int selectedPage = 0;
bool overlayMode = false;
int widthPx = 1;
int heightPx = 1;
float density = 1.f;
float touchX = -1.f;
float touchY = -1.f;
bool touching = false;
bool showFps = true;
bool compact = false;
bool showSample = true;
int page = 0;
int accent = 0;
float panelOpacity = .96f;
float borderRadius = 12.f;
float scale = 1.f;
std::chrono::steady_clock::time_point lastFrame;
const ImVec4 accents[4] = {
    ImVec4(.51f,.40f,.98f,1.f),
    ImVec4(.09f,.78f,.87f,1.f),
    ImVec4(.12f,.76f,.58f,1.f),
    ImVec4(1.f,.60f,.25f,1.f)
};

void applyTheme() {
    // Reset per-frame baseline so density scaling never accumulates over time.
    ImGuiStyle& style = ImGui::GetStyle();
    style = ImGuiStyle();
    ImGui::StyleColorsDark();
    style.WindowPadding = ImVec2(16.f, 14.f);
    style.FramePadding = ImVec2(12.f, 10.f);
    style.ItemSpacing = ImVec2(10.f, 12.f);
    style.ItemInnerSpacing = ImVec2(8.f, 7.f);
    style.WindowRounding = borderRadius;
    style.ChildRounding = borderRadius;
    style.FrameRounding = borderRadius * .65f;
    style.GrabRounding = 5.f;
    style.ScrollbarRounding = 8.f;
    style.WindowBorderSize = 1.f;
    style.FrameBorderSize = 0.f;
    style.Colors[ImGuiCol_WindowBg] = ImVec4(.065f,.090f,.150f,panelOpacity);
    style.Colors[ImGuiCol_ChildBg] = ImVec4(.105f,.135f,.215f,.96f);
    style.Colors[ImGuiCol_Text] = ImVec4(.95f,.96f,1.f,1.f);
    style.Colors[ImGuiCol_TextDisabled] = ImVec4(.57f,.65f,.77f,1.f);
    style.Colors[ImGuiCol_Border] = ImVec4(.27f,.32f,.45f,.95f);
    style.Colors[ImGuiCol_FrameBg] = ImVec4(.16f,.20f,.32f,1.f);
    style.Colors[ImGuiCol_FrameBgHovered] = ImVec4(.21f,.25f,.38f,1.f);
    style.Colors[ImGuiCol_FrameBgActive] = ImVec4(.28f,.29f,.43f,1.f);
    style.Colors[ImGuiCol_Button] = ImVec4(.16f,.19f,.31f,1.f);
    style.Colors[ImGuiCol_ButtonHovered] = accents[accent];
    style.Colors[ImGuiCol_ButtonActive] = accents[accent];
    style.Colors[ImGuiCol_CheckMark] = accents[accent];
    style.Colors[ImGuiCol_SliderGrab] = accents[accent];
    style.Colors[ImGuiCol_SliderGrabActive] = accents[accent];
    style.Colors[ImGuiCol_Header] = ImVec4(.27f,.23f,.45f,.85f);
    style.Colors[ImGuiCol_HeaderHovered] = accents[accent];
    style.Colors[ImGuiCol_HeaderActive] = accents[accent];
    style.Colors[ImGuiCol_TitleBgActive] = ImVec4(.16f,.15f,.28f,1.f);
    style.Colors[ImGuiCol_TabActive] = accents[accent];
    style.Colors[ImGuiCol_TabHovered] = accents[accent];
    style.Colors[ImGuiCol_Tab] = ImVec4(.12f,.16f,.26f,1.f);
    style.ScaleAllSizes(density * scale);
}

void menuToggle(const char* label, const char* detail, int bit) {
    int oldValue = overlayFlags.load();
    bool active = (oldValue & bit) != 0;
    if (ImGui::Checkbox(label, &active)) {
        overlayFlags.store(active ? (oldValue | bit) : (oldValue & ~bit));
    }
    ImGui::TextDisabled("%s", detail);
    ImGui::Spacing();
}

void moduleStatus(const char* name, const char* subtitle) {
    ImGui::Separator();
    ImGui::Text("%s", name);
    ImGui::SameLine();
    ImGui::TextColored(ImVec4(1.f, .70f, .32f, 1.f), "  UNAVAILABLE");
    ImGui::TextWrapped("%s", subtitle);
    ImGui::Spacing();
}

bool navButton(const char* title, int index, float height) {
    bool selected = selectedPage == index;
    if (selected) {
        ImGui::PushStyleColor(ImGuiCol_Button, accents[accent]);
        ImGui::PushStyleColor(ImGuiCol_Text, ImVec4(.05f,.04f,.10f,1.f));
    }
    bool clicked = ImGui::Button(title, ImVec2(-1.f, height));
    if (selected) ImGui::PopStyleColor(2);
    if (clicked) selectedPage = index;
    return clicked;
}

void renderMenu() {
    float width = std::max(1.f, std::min((float)widthPx, 510.f * density));
    float height = std::max(1.f,
            std::min((float)heightPx - 8.f * density, 630.f * density));
    ImGui::SetNextWindowPos(
            ImVec2(((float)widthPx - width) * .5f, ((float)heightPx - height) * .5f),
            ImGuiCond_Always);
    ImGui::SetNextWindowSize(ImVec2(width, height), ImGuiCond_Always);
    ImGuiWindowFlags flags = ImGuiWindowFlags_NoResize |
        ImGuiWindowFlags_NoCollapse | ImGuiWindowFlags_NoSavedSettings |
        ImGuiWindowFlags_NoMove | ImGuiWindowFlags_NoTitleBar;
    ImGui::Begin("##NOVA_PRIME_MENU", nullptr, flags);

    ImGui::TextColored(accents[accent], "NOVA  PRIME");
    ImGui::SameLine();
    ImGui::TextDisabled("  /  ARM64  /  5.0");
    ImGui::TextColored(ImVec4(.35f, .85f, .70f, 1.f),
            overlayMode ? "GAME SESSION MODE  -  READY" : "LOCAL UI PREVIEW");
    ImGui::Separator();

    float navWidth = 118.f * density;
    float buttonHeight = 42.f * density;
    ImGui::BeginChild("##left_nav", ImVec2(navWidth, 0.f), true);
    ImGui::TextDisabled("CONTROL CENTER");
    ImGui::Spacing();
    navButton("HOME", 0, buttonHeight);
    navButton("HUD", 1, buttonHeight);
    navButton("STYLE", 2, buttonHeight);
    navButton("MODULES", 3, buttonHeight);
    ImGui::Spacing();
    ImGui::Separator();
    ImGui::TextDisabled("ACTIVE WHILE");
    ImGui::TextWrapped("PUBG is foreground");
    ImGui::Spacing();
    ImGui::TextDisabled("Tap N bubble to close");
    ImGui::EndChild();

    ImGui::SameLine();
    ImGui::BeginChild("##content", ImVec2(0.f, 0.f), false);
    if (selectedPage == 0) {
        ImGui::TextColored(accents[accent], "SESSION DASHBOARD");
        ImGui::Separator();
        ImGui::TextWrapped("Premium native ImGui menu. Its bubble automatically appears when a supported PUBG game becomes foreground, and disappears after leaving the game.");
        ImGui::Spacing();
        ImGui::TextColored(ImVec4(.36f, .86f, .66f, 1.f), "CONNECTED: ANDROID OVERLAY");
        ImGui::TextWrapped("Crosshair, visual FOV ring, clock, session timer and theme settings have real local effects.");
        ImGui::Spacing();
        ImGui::TextColored(ImVec4(1.f, .72f, .35f, 1.f), "GAME PATCHES NOT CONNECTED");
        ImGui::TextWrapped("ESP, auto aim, recoil, skins and bypass from the old 4.5 code are NOT active in PUBG 4.7.");
        ImGui::Spacing();
        if ((overlayFlags.load() & 16) != 0) {
            ImGui::Text("UI renderer: %.1f FPS", ImGui::GetIO().Framerate);
        }
    } else if (selectedPage == 1) {
        ImGui::TextColored(accents[accent], "LIVE HUD CONTROLS");
        ImGui::Separator();
        ImGui::TextDisabled("These toggles change visible Android overlays.");
        ImGui::Spacing();
        menuToggle("Center crosshair", "Draw a non-interactive center sight.", 1);
        menuToggle("FOV guide ring", "Visual guide only; no aim assistance.", 2);
        menuToggle("Clock widget", "Show current device time during game.", 4);
        menuToggle("Session timer", "Count current foreground game session.", 8);
        ImGui::Separator();
        if (ImGui::Button("DISABLE ALL HUD", ImVec2(-1.f, 38.f*density))) {
            overlayFlags.fetch_and(~15);
        }
    } else if (selectedPage == 2) {
        ImGui::TextColored(accents[accent], "APPEARANCE");
        ImGui::Separator();
        const float aw = std::max(40.f, (ImGui::GetContentRegionAvail().x - 12.f) / 2.f);
        if (ImGui::Button("VIOLET", ImVec2(aw, 37.f*density))) accent = 0;
        ImGui::SameLine();
        if (ImGui::Button("CYAN", ImVec2(aw, 37.f*density))) accent = 1;
        if (ImGui::Button("EMERALD", ImVec2(aw, 37.f*density))) accent = 2;
        ImGui::SameLine();
        if (ImGui::Button("AMBER", ImVec2(aw, 37.f*density))) accent = 3;
        ImGui::Spacing();
        ImGui::SliderFloat("Opacity", &panelOpacity, .72f, 1.f, "%.2f");
        ImGui::SliderFloat("Corners", &borderRadius, 4.f, 24.f, "%.0f");
        ImGui::SliderFloat("UI scale", &scale, .80f, 1.15f, "%.2f");
        menuToggle("Compact layout", "Less spacing between controls.", 32);
        menuToggle("Show UI frame rate", "Measures ImGui renderer, not PUBG FPS.", 16);
        if (ImGui::Button("RESET STYLE", ImVec2(-1.f, 38.f*density))) {
            accent = 0;
            scale = 1.f;
            panelOpacity = .96f;
            borderRadius = 12.f;
            overlayFlags.fetch_and(~32);
        }
    } else {
        ImGui::TextColored(accents[accent], "GAME FEATURE STATUS");
        ImGui::Separator();
        ImGui::TextWrapped("Unverified legacy modules are shown as offline instead of switches that do nothing.");
        moduleStatus("ESP / PLAYERS", "Needs current 4.7 SDK, tested actor data and render integration.");
        moduleStatus("AIM / BULLET", "Not linked to PUBG 4.7; no game aim modification.");
        moduleStatus("RECOIL / WEAPONS", "Original weapon offsets not verified for 4.7.");
        moduleStatus("SKINS / VEHICLES", "No game runtime integration in this build.");
        moduleStatus("ANTI-BAN / BYPASS", "Cannot promise any ban prevention or safety.");
    }
    ImGui::EndChild();
    ImGui::End();
}
}

extern "C" JNIEXPORT void JNICALL
Java_com_RIYAZSRC_NativePreviewActivity_nativeInit(JNIEnv *, jclass, jfloat deviceDensity) {
    if (ready) {
        ImGui_ImplOpenGL3_Shutdown();
        ImGui::DestroyContext();
        ready = false;
    }
    density = std::max(.8f, std::min(4.f, (float)deviceDensity));
    overlayMode = false;
    touching = false;
    touchX = touchY = -1.f;
    ImGui::CreateContext();
    ImGuiIO& io = ImGui::GetIO();
    io.IniFilename = nullptr;
    io.LogFilename = nullptr;
    ImFontConfig font;
    font.SizePixels = 16.f * density;
    io.Fonts->AddFontDefault(&font);
    io.FontGlobalScale = 1.f;
    applyTheme();
    ready = ImGui_ImplOpenGL3_Init("#version 300 es");
    lastFrame = std::chrono::steady_clock::now();
}

extern "C" JNIEXPORT void JNICALL
Java_com_RIYAZSRC_NativePreviewActivity_nativeSetOverlayMode(JNIEnv *, jclass, jboolean enabled) {
    overlayMode = enabled == JNI_TRUE;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_RIYAZSRC_NativePreviewActivity_nativeGetOverlayFlags(JNIEnv *, jclass) {
    return (jint)overlayFlags.load();
}

extern "C" JNIEXPORT void JNICALL
Java_com_RIYAZSRC_NativePreviewActivity_nativeSetOverlayFlags(JNIEnv *, jclass, jint newFlags) {
    overlayFlags.store((int)newFlags & 63);
}

extern "C" JNIEXPORT void JNICALL
Java_com_RIYAZSRC_NativePreviewActivity_nativeResize(JNIEnv *, jclass, jint w, jint h) {
    widthPx = std::max(1, (int)w);
    heightPx = std::max(1, (int)h);
}

extern "C" JNIEXPORT void JNICALL
Java_com_RIYAZSRC_NativePreviewActivity_nativeTouch(JNIEnv *, jclass,
        jfloat x, jfloat y, jboolean pressed) {
    touchX = x;
    touchY = y;
    touching = pressed == JNI_TRUE;
}

extern "C" JNIEXPORT void JNICALL
Java_com_RIYAZSRC_NativePreviewActivity_nativeRender(JNIEnv *, jclass) {
    glViewport(0, 0, widthPx, heightPx);
    if (overlayMode) glClearColor(0.f, 0.f, 0.f, 0.f);
    else glClearColor(.027f, .041f, .080f, 1.f);
    glClear(GL_COLOR_BUFFER_BIT);
    if (!ready) return;
    ImGuiIO& io = ImGui::GetIO();
    io.DisplaySize = ImVec2((float)widthPx, (float)heightPx);
    io.DisplayFramebufferScale = ImVec2(1.f, 1.f);
    auto now = std::chrono::steady_clock::now();
    float delta = std::chrono::duration<float>(now - lastFrame).count();
    lastFrame = now;
    io.DeltaTime = std::max(.001f, std::min(.08f, delta));
    io.MousePos = touchX >= 0.f ? ImVec2(touchX, touchY) : ImVec2(-FLT_MAX, -FLT_MAX);
    io.MouseDown[0] = touching;
    ImGui_ImplOpenGL3_NewFrame();
    ImGui::NewFrame();
    applyTheme();
    if ((overlayFlags.load() & 32) != 0) {
        ImGui::GetStyle().ItemSpacing = ImVec2(5.f*density, 5.f*density);
    }
    renderMenu();
    ImGui::Render();
    ImGui_ImplOpenGL3_RenderDrawData(ImGui::GetDrawData());
}

extern "C" JNIEXPORT void JNICALL
Java_com_RIYAZSRC_NativePreviewActivity_nativeShutdown(JNIEnv *, jclass) {
    if (!ready) return;
    ImGui_ImplOpenGL3_Shutdown();
    ImGui::DestroyContext();
    ready = false;
}
