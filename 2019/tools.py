import functools
import os
import requests

SESSION_KEY = '53616c7465645f5f1a9e96709720d654becc56ffe8ec8b8b8baf6cfe66cdd95be28d3386f972c08c1cdb5e3225e01f6f'
COOKIES = {'session': SESSION_KEY}
DATA_DIR = os.path.join(os.path.dirname(__file__), '.data')

try:
    os.mkdir(DATA_DIR)
except:
    pass

def data_url(day, key):
    return f'https://adventofcode.com/2019/day/{day}/{key}'

@functools.lru_cache()
def get_data(day, key='input'):
    data_file_key = f'{day}_{key}'
    stored_data_filename = os.path.join(DATA_DIR, data_file_key)
    try:
        with open(stored_data_filename) as stored_data:
            return stored_data.read()
    except FileNotFoundError:
        response = requests.get(data_url(day, key), cookies=COOKIES)
        assert response.ok
        with open(stored_data_filename, 'w') as stored_data:
            stored_data.write(response.text)
        return response.text
