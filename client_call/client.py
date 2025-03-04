import asyncio
import websockets
import nest_asyncio
import io
import pygame
import pyaudio
from quart import Quart, Response
import tkinter as tk
from threading import Thread
import numpy as np
import datetime
from PIL import Image, ImageTk
import os

# Tkinter 시스템 경고 메시지 방지 (MacOS)
os.environ["TK_SILENCE_DEPRECATION"] = "1"

# nest_asyncio를 사용하여 이미 실행 중인 이벤트 루프에서 비동기 작업을 허용
nest_asyncio.apply()
app = Quart(__name__)
pygame.mixer.init()

# PyAudio 설정
FORMAT = pyaudio.paInt16
CHANNELS = 1
RATE = 16000
CHUNK = 1024
target_client_uri = "ws://172.19.87.52:8766"
receive_text = "init"


# ✅ UI 클래스 (메인 스레드에서 실행)
class UI(tk.Tk):
    def __init__(self):
        super().__init__()
        self.title("MicroSIP")
        self.geometry("500x400")

        # ✅ Tkinter 위젯 생성 (기존 UI 요소 추가)
        self.create_widgets()

    def create_widgets(self):
        """ UI 요소를 생성하는 함수 (기존 버튼 및 입력창 포함) """
        self.label = tk.Label(self, text="MicroSIP 클라이언트", font=("Arial", 16))
        self.label.pack(pady=5)

        # ✅ 전화번호 입력창
        self.entry = tk.Entry(self, font=("Arial", 16), justify='left', width=19)
        self.entry.pack(pady=5)

        # ✅ 숫자 키패드 (기존 UI 복원)
        buttons = [
            ('1', ''), ('2', 'ABC'), ('3', 'DEF'),
            ('4', 'GHI'), ('5', 'JKL'), ('6', 'MNO'),
            ('7', 'PQRS'), ('8', 'TUV'), ('9', 'WXYZ'),
            ('*', ''), ('0', ''), ('#', '')
        ]

        button_frame = tk.Frame(self)
        button_frame.pack()

        row, col = 0, 0
        for (num, chars) in buttons:
            btn = tk.Button(button_frame, text=num, font=("Arial", 14), width=5, height=2,
                            command=lambda b=num: self.on_button_click(b))
            btn.grid(row=row, column=col, padx=2, pady=2)
            col += 1
            if col > 2:
                col = 0
                row += 1

        # ✅ 종료 버튼
        self.quit_button = tk.Button(self, text="종료", command=self.exit_program, fg="white", bg="red", font=("Arial", 12))
        self.quit_button.pack(pady=10)

    def on_button_click(self, value):
        """ 번호 키패드 버튼 클릭 시 입력창에 추가 """
        current_text = self.entry.get()
        self.entry.delete(0, tk.END)
        self.entry.insert(0, current_text + value)

    def exit_program(self):
        """ UI 종료 함수 """
        self.destroy()  # UI 창 종료
        pygame.mixer.quit()  # Pygame 종료
        stream.stop_stream()
        stream.close()
        audio.terminate()
        exit(0)


# ✅ UI를 메인 스레드에서 실행
def start_ui():
    global ui
    ui = UI()
    ui.mainloop()


# ✅ PyAudio 스트리밍 설정
audio = pyaudio.PyAudio()
stream = audio.open(
    format=FORMAT, channels=CHANNELS, rate=RATE, input=True, frames_per_buffer=CHUNK
)


def generate_audio():
    while True:
        data = stream.read(CHUNK)
        yield data


@app.route("/stream", methods=["GET"])
def stream_audio():
    return Response(generate_audio(), mimetype="audio/wav")


# ✅ WebSockets 서버 설정
async def receive_audio(websocket, path):
    global receive_text
    print("Waiting for audio data...")
    async for message in websocket:
        if isinstance(message, bytes):
            audio_fp = io.BytesIO(message)
            audio_fp.seek(0)
            pygame.mixer.music.load(audio_fp, "mp3")
            pygame.mixer.music.play()
        else:
            receive_text = message


async def start_server():
    print("WebSocket 서버 시작 중...")
    server = await websockets.serve(receive_audio, "0.0.0.0", 8765)
    await server.wait_closed()


# ✅ 실행 흐름 정리
async def run_quart():
    await app.run_task(debug=True, host="0.0.0.0", port=8766)


async def main():
    """ 메인 실행 함수 """
    # ✅ UI를 메인 스레드에서 실행
    ui_thread = Thread(target=start_ui, daemon=True)
    ui_thread.start()

    # ✅ Quart 서버와 WebSockets 서버를 실행
    await asyncio.gather(run_quart(), start_server())


# ✅ 메인 실행 (asyncio 루프 설정)
if __name__ == "__main__":
    start_ui()  # ✅ Tkinter를 메인 스레드에서 실행
    asyncio.run(main())  # ✅ 나머지 비동기 서버 실행
