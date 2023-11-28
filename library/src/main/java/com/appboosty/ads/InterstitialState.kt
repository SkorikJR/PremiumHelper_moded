package com.appboosty.ads

class InterstitialState {

    sealed class State {
        object NotShown : State()
        object Requested : State()
        object Shown : State()
    }

    private var state : State = State.NotShown

    fun onInterstitialRequested(){
        state = State.Requested
    }

    fun onInterstitialShown(){
        state = State.Shown
    }

    fun onInterstitialHidden(){
        state = State.NotShown
    }

    fun isAllowedToShow() = state == State.NotShown
}