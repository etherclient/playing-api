#include <windows.h>
#include <winrt/Windows.Media.Control.h>
#include <winrt/Windows.Storage.Streams.h>
#include <winrt/Windows.Foundation.h>
#include <cstdlib>
#include <cstring>

using namespace winrt;
using namespace Windows::Media::Control;
using namespace Windows::Storage::Streams;

static GlobalSystemMediaTransportControlsSessionManager g_mgr{ nullptr };
static GlobalSystemMediaTransportControlsSession g_sess{ nullptr };

static void InitOnce() {
    static bool initialized = false;
    if (!initialized) {
        init_apartment(apartment_type::multi_threaded);
        try {
            g_mgr = GlobalSystemMediaTransportControlsSessionManager::RequestAsync().get();
            if (g_mgr) g_sess = g_mgr.GetCurrentSession();
        } catch (...) { }
        initialized = true;
    }
}

static wchar_t* CopyHString(hstring const& s) {
    if (s.empty()) return nullptr;
    size_t bytes = (s.size() + 1) * sizeof(wchar_t);
    wchar_t* mem = (wchar_t*)malloc(bytes);
    memcpy(mem, s.c_str(), bytes);
    return mem;
}

static void* ReadImage(IRandomAccessStreamReference const& ref, int* size) {
    *size = 0;
    if (!ref) return nullptr;
    try {
        auto stream = ref.OpenReadAsync().get();
        uint32_t len = (uint32_t)stream.Size();
        Buffer buffer(len);
        stream.ReadAsync(buffer, len, InputStreamOptions::None).get();
        void* mem = malloc(len);
        memcpy(mem, buffer.data(), len);
        *size = (int)len;
        return mem;
    } catch (...) {
        return nullptr;
    }
}

// strings
extern "C" __declspec(dllexport)
wchar_t* __cdecl getTitle() {
    InitOnce();
    if (!g_sess) return nullptr;
    try {
        auto props = g_sess.TryGetMediaPropertiesAsync().get();
        return CopyHString(props.Title());
    } catch (...) { return nullptr; }
}

extern "C" __declspec(dllexport)
wchar_t* __cdecl getArtist() {
    InitOnce();
    if (!g_sess) return nullptr;
    try {
        auto props = g_sess.TryGetMediaPropertiesAsync().get();
        return CopyHString(props.Artist());
    } catch (...) { return nullptr; }
}

extern "C" __declspec(dllexport)
wchar_t* __cdecl getAlbum() {
    InitOnce();
    if (!g_sess) return nullptr;
    try {
        auto props = g_sess.TryGetMediaPropertiesAsync().get();
        return CopyHString(props.AlbumTitle());
    } catch (...) { return nullptr; }
}

// played position
extern "C" __declspec(dllexport)
int __cdecl getPlayedSeconds() {
    InitOnce();
    if (!g_sess) return 0;
    try {
        auto timeline = g_sess.GetTimelineProperties();
        auto pos = timeline.Position();
        return static_cast<int>(pos.count() / 10'000'000);
    } catch (...) { return 0; }
}

// duration via timeline
extern "C" __declspec(dllexport)
int __cdecl getDurationSeconds() {
    InitOnce();
    if (!g_sess) return 0;
    try {
        auto timeline = g_sess.GetTimelineProperties();
        auto start = timeline.StartTime().count();
        auto end   = timeline.EndTime().count();
        if (end > start) {
            return static_cast<int>((end - start) / 10'000'000);
        }
    } catch (...) {}
    return 0;
}

extern "C" __declspec(dllexport)
void* __cdecl getAlbumImage(int* size) {
    InitOnce();
    if (!g_sess || !size) return nullptr;

    try {
        auto props = g_sess.TryGetMediaPropertiesAsync().get();
        auto thumb = props.Thumbnail();
        if (!thumb) return nullptr;

        auto stream = thumb.OpenReadAsync().get();
        uint32_t len = (uint32_t)stream.Size();

        Buffer buffer(len);
        stream.ReadAsync(buffer, len, InputStreamOptions::None).get();

        void* mem = malloc(len);
        memcpy(mem, buffer.data(), len);
        *size = (int)len;
        return mem;
    } catch (...) {
        return nullptr;
    }
}

extern "C" __declspec(dllexport)
bool __cdecl isAlbumImageAvailable() {
    InitOnce();
    if (!g_sess) return false;
    try {
        auto props = g_sess.TryGetMediaPropertiesAsync().get();
        return props.Thumbnail() != nullptr;
    } catch (...) { }
    return false;
}

extern "C" __declspec(dllexport)
void* __cdecl getArtistImage(int* size) {
    return nullptr;
}

extern "C" __declspec(dllexport)
bool __cdecl isArtistImageAvailable() {
    return false;
}

extern "C" __declspec(dllexport)
void __cdecl freeMemory(void* ptr) {
    free(ptr);
}
