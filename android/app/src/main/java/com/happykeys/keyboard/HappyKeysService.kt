package com.happykeys.keyboard

import android.inputmethodservice.InputMethodService
import android.text.InputType
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo

class HappyKeysService : InputMethodService(), KeyListener {
    private var view: KeyboardView? = null
    private var sound: SoundEngine? = null
    private var song: List<Double> = Songs.all.values.first()
    private var songName = ""
    private var soundOn = true
    private var idx = 0

    override fun onCreate() {
        super.onCreate()
        sound = SoundEngine(this)
    }

    override fun onCreateInputView(): View {
        val v = KeyboardView(this, this)
        view = v
        return v
    }

    override fun onStartInputView(info: EditorInfo, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        val p = getSharedPreferences("prefs", MODE_PRIVATE)
        val name = p.getString("song", Songs.names.first()) ?: Songs.names.first()
        soundOn = p.getBoolean("sound", true)
        if (name != songName) {
            songName = name
            song = Songs.all[name] ?: Songs.all.values.first()
            idx = 0
        }
        val cls = info.inputType and InputType.TYPE_MASK_CLASS
        view?.setSymbols(cls == InputType.TYPE_CLASS_NUMBER || cls == InputType.TYPE_CLASS_PHONE)
    }

    override fun onDestroy() {
        sound?.release()
        super.onDestroy()
    }

    override fun onText(text: String) {
        currentInputConnection?.commitText(text, 1)
    }

    override fun onBackspace() {
        sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL)
    }

    override fun onEnter() {
        val ei = currentInputEditorInfo
        val action = ei?.imeOptions?.and(EditorInfo.IME_MASK_ACTION) ?: EditorInfo.IME_ACTION_NONE
        val noEnterAction = ((ei?.imeOptions ?: 0) and EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0
        if (!noEnterAction && action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED) {
            currentInputConnection?.performEditorAction(action)
        } else {
            sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER)
        }
    }

    override fun onTune() {
        if (!soundOn || song.isEmpty()) return
        sound?.play(song[idx % song.size])
        idx++
    }
}
