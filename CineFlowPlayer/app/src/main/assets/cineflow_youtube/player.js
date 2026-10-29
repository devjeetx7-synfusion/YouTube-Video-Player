(function () {
  "use strict";

  var player = null;
  var ready = false;
  var playerState = -1;
  var errorCode = 0;
  var seeking = false;
  var lastDuration = 0;

  var shell = document.getElementById("player-shell");
  var loading = document.getElementById("loading-indicator");
  var errorBox = document.getElementById("error-message");
  var errorDetail = document.getElementById("error-detail");
  var centerPlay = document.getElementById("center-play");
  var playButton = document.getElementById("play-pause");
  var muteButton = document.getElementById("mute-button");
  var fullscreenButton = document.getElementById("fullscreen-button");
  var seekbar = document.getElementById("seekbar");
  var currentTime = document.getElementById("current-time");
  var totalTime = document.getElementById("total-time");

  function formatTime(seconds) {
    seconds = Math.max(0, Math.floor(Number(seconds) || 0));
    var h = Math.floor(seconds / 3600);
    var m = Math.floor((seconds % 3600) / 60);
    var s = seconds % 60;
    return h > 0
      ? h + ":" + String(m).padStart(2, "0") + ":" + String(s).padStart(2, "0")
      : String(m).padStart(2, "0") + ":" + String(s).padStart(2, "0");
  }

  function updateControls() {
    if (!player || !ready) return;
    var duration = Number(player.getDuration()) || 0;
    var position = Number(player.getCurrentTime()) || 0;
    lastDuration = duration;
    if (!seeking) {
      seekbar.value = duration > 0 ? String(Math.round(position / duration * 1000)) : "0";
      currentTime.textContent = formatTime(position);
    }
    totalTime.textContent = formatTime(duration);
    var playing = playerState === YT.PlayerState.PLAYING;
    playButton.textContent = playing ? "Ⅱ" : "▶";
    playButton.setAttribute("aria-label", playing ? "Pause" : "Play");
    centerPlay.hidden = playing || playerState === YT.PlayerState.BUFFERING;
    loading.hidden = playerState !== YT.PlayerState.BUFFERING && playerState !== -1;
  }

  function onPlayerReady() {
    ready = true;
    loading.hidden = true;
    errorBox.hidden = true;
    updateControls();
  }

  function onPlayerStateChange(event) {
    playerState = event.data;
    if (event.data === YT.PlayerState.BUFFERING) loading.hidden = false;
    else loading.hidden = true;
    if (event.data === YT.PlayerState.PLAYING) {
      errorBox.hidden = true;
      errorCode = 0;
    }
    updateControls();
  }

  function onPlayerError(event) {
    errorCode = Number(event.data) || 1;
    loading.hidden = true;
    errorBox.hidden = false;
    var messages = {
      2: "The video ID is invalid.",
      5: "The video could not be played by the HTML5 player.",
      100: "The video was removed or is unavailable.",
      101: "The video owner does not allow embedded playback.",
      150: "The video owner does not allow embedded playback."
    };
    errorDetail.textContent = messages[errorCode] || "YouTube could not play this video (error " + errorCode + ").";
    updateControls();
  }

  function initializeYouTubeApi() {
    if (window.YT && window.YT.Player) {
      createPlayer();
      return;
    }
    var tag = document.createElement("script");
    tag.src = "https://www.youtube.com/iframe_api";
    tag.onerror = function () {
      loading.hidden = true;
      errorBox.hidden = false;
      errorDetail.textContent = "Could not load YouTube. Check your internet connection and try again.";
      errorCode = -1;
    };
    var first = document.getElementsByTagName("script")[0];
    first.parentNode.insertBefore(tag, first);
  }

  function createPlayer() {
    if (player) return;
    player = new YT.Player("youtube-player", {
      width: "100%",
      height: "100%",
      videoId: "",
      playerVars: {
        autoplay: 0,
        controls: 0,
        playsinline: 1,
        rel: 0,
        fs: 0
      },
      events: {
        onReady: onPlayerReady,
        onStateChange: onPlayerStateChange,
        onError: onPlayerError
      }
    });
  }

  window.onYouTubeIframeAPIReady = createPlayer;

  playButton.addEventListener("click", function () {
    if (!ready || !player) return;
    if (playerState === YT.PlayerState.PLAYING) player.pauseVideo();
    else player.playVideo();
  });
  centerPlay.addEventListener("click", function () {
    if (ready && player) player.playVideo();
  });
  seekbar.addEventListener("input", function () {
    seeking = true;
    var duration = ready && player ? Number(player.getDuration()) || lastDuration : lastDuration;
    currentTime.textContent = formatTime(duration * Number(seekbar.value) / 1000);
  });
  seekbar.addEventListener("change", function () {
    if (ready && player) {
      var duration = Number(player.getDuration()) || lastDuration;
      if (duration > 0) player.seekTo(duration * Number(seekbar.value) / 1000, true);
    }
    seeking = false;
    updateControls();
  });
  muteButton.addEventListener("click", function () {
    if (!ready || !player) return;
    if (player.isMuted()) {
      player.unMute();
      muteButton.textContent = "🔊";
      muteButton.setAttribute("aria-label", "Mute audio");
    } else {
      player.mute();
      muteButton.textContent = "🔇";
      muteButton.setAttribute("aria-label", "Unmute audio");
    }
  });

  fullscreenButton.addEventListener("click", function () {
    try {
      if (document.fullscreenElement) {
        document.exitFullscreen();
        fullscreenButton.setAttribute("aria-label", "Enter fullscreen");
      } else if (shell.requestFullscreen) {
        shell.requestFullscreen();
        fullscreenButton.setAttribute("aria-label", "Exit fullscreen");
      }
    } catch (_) {
      // Some WebView versions do not expose the Fullscreen API.
    }
  });
  document.addEventListener("fullscreenchange", function () {
    fullscreenButton.setAttribute("aria-label", document.fullscreenElement ? "Exit fullscreen" : "Enter fullscreen");
  });

  window.CineFlowPlayer = {
    isReady: function () { return (!!ready && !!player) || errorCode !== 0; },
    loadVideo: function (videoId, autoplay, startSeconds) {
      if (!ready || !player || !/^[A-Za-z0-9_-]{11}$/.test(String(videoId || ""))) return false;
      errorCode = 0;
      errorBox.hidden = true;
      loading.hidden = false;
      playerState = -1;
      seekbar.value = "0";
      currentTime.textContent = "00:00";
      totalTime.textContent = "00:00";
      var options = { videoId: String(videoId), startSeconds: Math.max(0, Number(startSeconds) || 0) };
      if (autoplay) player.loadVideoById(options);
      else player.cueVideoById(options);
      return true;
    },
    play: function () { if (ready && player) player.playVideo(); },
    pause: function () { if (ready && player) player.pauseVideo(); },
    seekTo: function (seconds) { if (ready && player) player.seekTo(Math.max(0, Number(seconds) || 0), true); },
    getSnapshot: function () {
      var duration = ready && player ? Number(player.getDuration()) || 0 : 0;
      var position = ready && player ? Number(player.getCurrentTime()) || 0 : 0;
      return JSON.stringify({
        ready: (!!ready && !!player) || errorCode !== 0,
        state: playerState,
        error: errorCode,
        duration: duration,
        position: position,
        playing: playerState === 1
      });
    }
  };

  window.setInterval(updateControls, 250);
  initializeYouTubeApi();
})();