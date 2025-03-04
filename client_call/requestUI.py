from http.server import BaseHTTPRequestHandler, HTTPServer
import json
from tkinter import Tk, Label, Button, PhotoImage
import subprocess
import sys
import threading
import tkinter as tk
import pygame
from PIL import Image, ImageTk


def play_music():
    pygame.mixer.init()
    pygame.mixer.music.load("bell.mp3")
    pygame.mixer.music.play()

class RequestHandler(BaseHTTPRequestHandler):
        
    def do_POST(self):
        try:
            content_length = int(self.headers['Content-Length'])
            post_data = self.rfile.read(content_length)
            data = json.loads(post_data)

            if data['type'] == 'call_request':
                play_music() 
                response = self.show_accept_decline_prompt()
                pygame.mixer.music.stop()
                
                if response == 'accept':
                    self.send_response(200)
                    self.send_header('Content-type', 'application/json')
                    self.end_headers()
                    self.wfile.write(json.dumps({'status': 'accepted'}).encode())
                    
                    self.start_call_script()
                else:
                    self.send_response(200)
                    self.send_header('Content-type', 'application/json')
                    self.end_headers()
                    self.wfile.write(json.dumps({'status': 'declined'}).encode())
                    #창 닫고 다시 코드 돌리는 쪽으로 구현하면 좋겠음.

        except Exception as e:
            self.send_response(500)
            self.end_headers()
            print(f"Error handling POST request: {e}", file=sys.stderr)

    def do_GET(self):
        self.send_response(200)
        self.send_header('Content-type', 'text/html')
        self.end_headers()
        self.wfile.write(b"GET request received")

    def show_accept_decline_prompt(self): #통신 요청 팝업 창
        root = Tk()
        root.title("수신 전화")
        root.geometry("320x400")
        result = {'choice': None}
        client_ip, client_port=self.client_address

        def accept_call():
            result['choice'] = 'accept'
            root.destroy()

        def decline_call():
            result['choice'] = 'decline'
            root.destroy()

        # 전화번호 라벨
        phone_number = "010-3125-3041"
        phone_label = Label(root, text=phone_number, font=("Helvetica", 14))
        phone_label.pack(pady=50)

        # 이미지 로드
        image = Image.open("call_profile.png")
        image = image.resize((150, 150), Image.Resampling.LANCZOS)
        photo = ImageTk.PhotoImage(image)

        # 이미지 라벨
        image_label = Label(root, image=photo)
        image_label.image = photo  # 레퍼런스를 유지합니다.
        image_label.pack(pady=10)
        
        #Label(root, text=f"{client_ip}로 부터 수신").pack(pady=5)
        Button(root, text="응답", command=accept_call).place(x=5, y=350, width=150, height=40)
        Button(root, text="거절", command=decline_call).place(x=165, y=350, width=150, height=40)

        root.mainloop()
        return result['choice']

    def start_call_script(self):
        try:
            # 절대 경로를 사용하여 call.py 스크립트를 실행
            script_path = '/Users/gimhanbyeol/Downloads/project/code/client_call/client.py'  # temp.py 파일의 절대 경로
            process = subprocess.Popen(
                ['python', script_path],
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
                text=True
            )
            stdout, stderr = process.communicate()
            
            # 표준 출력과 표준 오류를 로그로 출력
            print(f"stdout: {stdout}")
            print(f"stderr: {stderr}")

        except Exception as e:
            print(f"Failed to start call script: {e}", file=sys.stderr)


def run_server():
    server_address = ('0.0.0.0', 8767)
    httpd = HTTPServer(server_address, RequestHandler)
    print('Running server...')
    httpd.serve_forever()

if __name__ == '__main__':
    run_server()