from quart import Quart, request, jsonify
import openai
from openai import OpenAI
import io
import asyncio
import websockets
import nest_asyncio

key = ""
openai.api_key = key
client = OpenAI(api_key=key)

app = Quart(__name__)

# 메시지를 전송할 클라이언트의 URI
target_client_uri = "ws://192.168.135.146:8765"

nest_asyncio.apply()
trans_mode = 0
filter_command = "욕설 및 비하표현을 X로 바꿔줘. 예를 들어 '개새끼'는 XXX로 바꾸고 '시발'은 XX로 바꿔서 글자수를 동일하게 맞춰야해."
command_3 = "민원인의 말을 상담사에게 전달할 예정이다. 말에 답하지 말고 욕설은 순화하고 하대하는 말투는 높힘말로 부드럽게 전달하라"
command_4 = "민원인의 말을 상담사에게 전달할 예정이다. 말에 답하지 말고 욕설은 순화하고 하대하는 말투는 높힘말로 부드럽게 전달하라"
chatgpt_command = [command_3, command_4, filter_command, " "]
chatgpt_model = ["ft:gpt-3.5-turbo-1106:lee:i-message:9ajO4SF9", "gpt-4-turbo", "ft:gpt-3.5-turbo-1106:lee:filteringfinal:9bqcBgqu", " "]

def Ask_Chatgpt(text):
    if trans_mode == 3:
        return text
    
    conversation = []
    conversation.append({"role": "system", "content": chatgpt_command[trans_mode]})
    conversation.append({"role": "user", "content": text})
    response = openai.chat.completions.create(
        model=chatgpt_model[trans_mode],
        messages=conversation
    )
    return response.choices[0].message.content

# 클라이언트로 음성 데이터를 전송하는 함수
async def send_audio_to_client(audio_data):
    async with websockets.connect(target_client_uri) as websocket:
        await websocket.send(audio_data.read())
        print("Sent audio data to client")
        
# 클라이언트로 텍스트 데이터를 전송하는 함수 추가
async def send_text_to_client(text_data):
    async with websockets.connect(target_client_uri) as websocket:
        await websocket.send(text_data)
        print("Sent text data to client")

@app.route('/receive', methods=['POST'])
async def receive_text():
    data = await request.get_json()
    if not data or 'text' not in data:
        return jsonify({'error': 'No text data provided'}), 400
    received_text = data['text']
    print(f"Received text: {received_text}")
    
    response = Ask_Chatgpt(received_text)
    print(f"Asked text: {response}")
    
    # OpenAI TTS API를 사용하여 텍스트를 음성 데이터로 변환
    response_voice = client.audio.speech.create(
        model="tts-1",
        voice="alloy",
        input=response
    )
    audio_fp = io.BytesIO()
    audio_fp.write(response_voice.content)
    audio_fp.seek(0)

    # 비동기로 음성 데이터를 클라이언트로 전송
    await send_audio_to_client(audio_fp)
    return jsonify({'status': 'success', 'received_text': received_text})

async def receive_number(websocket, path):
    global trans_mode
    print("Waiting for number data...")
    async for message in websocket:
        try:
            trans_mode = int(message)
            print(f"mode changed : {trans_mode}")
        except ValueError:
            print(f"Invalid number received: {message}")

async def start_server():
    print("Starting WebSocket server...")
    server = await websockets.serve(receive_number, "0.0.0.0", 8766)
    await server.wait_closed()

if __name__ == '__main__':
    loop = asyncio.get_event_loop()
    app_task = loop.create_task(app.run_task(debug=True, host='0.0.0.0', port=8765))
    server_task = loop.create_task(start_server())
    loop.run_forever()
