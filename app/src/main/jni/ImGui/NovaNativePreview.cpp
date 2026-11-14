// NOVA standalone preview for Dear ImGui (same ImGui version as the original app).
// This is a UI/render/input test only: no hooking, no game memory, no anti-cheat modifications.
#include "imgui.h"
#include "imgui_impl_opengl3.h"
#include <jni.h>
#include <GLES3/gl3.h>
#include <chrono>
#include <cmath>
#include <algorithm>

namespace {
bool ready = false;
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
    ImGui::StyleColorsDark();
    ImGuiStyle& style = ImGui::GetStyle();
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

void renderMenu() {
    // Fill the screen at mobile density, remain legible in landscape/portrait.
    float available = std::min((float)widthPx, 460.f * density);
    float height = std::min((float)heightPx - 24.f*density, 640.f*density);
    ImGui::SetNextWindowPos(ImVec2((widthPx-available)/2.f, (heightPx-height)/2.f), ImGuiCond_Always);
    ImGui::SetNextWindowSize(ImVec2(available, height), ImGuiCond_Always);
    ImGuiWindowFlags flags = ImGuiWindowFlags_NoResize | ImGuiWindowFlags_NoCollapse |
        ImGuiWindowFlags_NoSavedSettings | ImGuiWindowFlags_NoMove;
    ImGui::Begin("NOVA | Native ImGui", nullptr, flags);
    ImGui::TextColored(accents[accent], "NOVA  /  IMGUI ENGINE");
    ImGui::TextDisabled("Compiled C++ renderer, real touch input");
    ImGui::Separator();

    const float buttonWidth = (ImGui::GetContentRegionAvail().x - 12.f*density)/3.f;
    if (ImGui::Button("Home", ImVec2(buttonWidth, 41.f*density))) page = 0;
    ImGui::SameLine();
    if (ImGui::Button("Style", ImVec2(buttonWidth, 41.f*density))) page = 1;
    ImGui::SameLine();
    if (ImGui::Button("Test", ImVec2(buttonWidth, 41.f*density))) page = 2;
    ImGui::Spacing();

    if (page == 0) {
        ImGui::TextWrapped("This is the real Dear ImGui renderer from the original source repository.");
        ImGui::Spacing();
        ImGui::TextColored(ImVec4(.30f,.85f,.68f,1.f), "C++ RENDERING ACTIVE");
        ImGui::BulletText("OpenGL ES 3.0: active");
        ImGui::BulletText("Touch interaction: active");
        ImGui::BulletText("Original native menu source: packaged separately");
        ImGui::Spacing();
        ImGui::TextWrapped("No game process access, injection, ESP or aim modification occurs.");
    } else if (page == 1) {
        ImGui::Text("Appearance settings");
        ImGui::Separator();
        if (ImGui::Button("Violet", ImVec2(buttonWidth, 38.f*density))) accent=0;
        ImGui::SameLine();
        if (ImGui::Button("Cyan", ImVec2(buttonWidth, 38.f*density))) accent=1;
        ImGui::SameLine();
        if (ImGui::Button("Green", ImVec2(buttonWidth, 38.f*density))) accent=2;
        if (ImGui::Button("Orange", ImVec2(buttonWidth, 38.f*density))) accent=3;
        ImGui::SliderFloat("Opacity", &panelOpacity, .72f, 1.f, "%.2f");
        ImGui::SliderFloat("Corners", &borderRadius, 0.f, 22.f, "%.0f");
        ImGui::Checkbox("Compact layout", &compact);
        ImGui::TextDisabled("Settings affect only this preview.");
    } else {
        ImGui::Text("Working input tests");
        ImGui::Separator();
        ImGui::Checkbox("Show frame rate", &showFps);
        ImGui::Checkbox("Show sample panel", &showSample);
        static float testSlider = .50f;
        ImGui::SliderFloat("Slider", &testSlider, 0.f, 1.f);
        if (showSample) ImGui::ProgressBar(testSlider, ImVec2(-1.f, 0.f));
        ImGui::TextDisabled("Drag and tap to confirm touch handling.");
    }
    ImGui::Separator();
    if (showFps) ImGui::Text("FPS  %.1f", ImGui::GetIO().Framerate);
    ImGui::TextDisabled("Native UI preview only - no PUBG support verified");
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
    glClearColor(.027f, .041f, .080f, 1.f);
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
    if (compact) {
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
