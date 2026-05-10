import time
import threading
from pynput.keyboard import Key, Controller, Listener

keyboard = Controller()
running = True

def spam_space():
    while running:
        time.sleep(10)
        keyboard.press(Key.space)
        keyboard.release(Key.space)

def on_press(key):
    global running
    try:
        if key.char == 'q':
            running = False
            return False
    except:
        pass

t = threading.Thread(target=spam_space)
t.start()

with Listener(on_press=on_press) as listener:
    listener.join()

t.join()
