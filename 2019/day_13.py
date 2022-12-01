import curses
import enum
import time
import threading
import typing

from intcode import *
import tools

import logging

program = make_tape(tools.get_data(13))
hacked_program = [2] + program[1:]


SCREEN_X, SCREEN_Y = 37, 22


class StructuredEnv:
    INPUT_STRUCTURE = None
    OUTPUT_STRUCTURE = None

    def __init__(self, initial_input=None):
        self._initial_input = initial_input

    def reset(self):
        if self._initial_input:
            self.send(self._initial_input)
        else:
            self.input_state = None
        self.reset_output()

    def reset_output(self):
        self._output_state = output_structure_state(self.OUTPUT_STRUCTURE)
        next(self._output_state)  # kick

    def input(self):
        if self._input_state is None:
            raise Exception("No input to consume")
        try:
            return self._input_state.next()
        except StopIteration:
            self._input_state = None
            return self.input()

    def output(self, value):
        try:
            result = self._output_state.send(value)
            if result is not None:
                self.emit(result)
        except StopIteration:
            self.reset_output()
            return self.output(value)

    def emit(self, value):
        raise NotImplemented

    def send(self, value):
        self._input_state = input_structure_state(self.INPUT_STRUCTURE, value)


class ArcadeBlock(enum.Enum):
    EMPTY = 0
    WALL = 1
    BLOCK = 2
    PADDLE = 3
    BALL = 4


class ArcadeOutput(typing.NamedTuple):
    x: int
    y: int
    block: ArcadeBlock


class ArcadeScore(typing.NamedTuple):
    score: int


def game_output_state():
    data = []
    for _ in range(3):
        data.append((yield))
    if data[0:2] == [-1, 0]:
        yield ArcadeScore(data[2])
    else:
        yield ArcadeOutput(data[0], data[1], ArcadeBlock(data[2]))


def play_game(window):
    #window = curses.newwin(SCREEN_Y + 5, SCREEN_X + 4, 0, 0)
    window.keypad(1)  # Return special KEY_LEFT, etc.
    window.nodelay(1) # don't block on asking for input

    JOYSTICK_POSITION = 0

    def watch_for_joystick():
        nonlocal JOYSTICK_POSITION
        while True:
            key = window.getch()
            if key != -1:
                logging.info("Joystick thread found key %s", key)
            if key == curses.KEY_RIGHT:
                JOYSTICK_POSITION = 1
            elif key == curses.KEY_LEFT:
                JOYSTICK_POSITION = -1
            window.addstr(SCREEN_Y + 2, 1, f'Pressing {JOYSTICK_POSITION} ')

    def get_joystick_position():
        nonlocal JOYSTICK_POSITION
        position = JOYSTICK_POSITION
        JOYSTICK_POSITION = 0
        return position

    joystick_thread = threading.Thread(target=watch_for_joystick, daemon=True)
    joystick_thread.start()

    class PlayArcadeGame(StructuredEnv):
        REFRESH_RATE = 1

        BLOCK_ICONS = {
            ArcadeBlock.EMPTY: ' ',
            ArcadeBlock.WALL: '#',
            ArcadeBlock.BLOCK: '.',
            ArcadeBlock.PADDLE: '=',
            ArcadeBlock.BALL: 'O',
        }

        def reset(self):
            super().reset()
            self.screen = {}
            self.score = None
            self.joystick_position = 0
            self.last_refresh = time.time()

        def emit(self, value: ArcadeOutput):
            if isinstance(value, ArcadeScore):
                self.score = value.score
            else:
                self.screen[(value.x, value.y)] = value.block

        def input(self):
            # Assumption: Input updated exactly once per refresh
            self.refresh_screen()
            # Get input after a screen refresh so any keys seen affect position
            position = int(get_joystick_position())
            logging.info(f"Sending input {position}")
            return position

        def reset_output(self):
            self._output_state = game_output_state()
            next(self._output_state)  # kick

        def refresh_screen(self):
            logging.debug("Refreshing screen")
            sleep_time = (1 / self.REFRESH_RATE) - (time.time() - self.last_refresh)
            if sleep_time > 0:
                logging.debug(f"Sleeping for {sleep_time}")
                time.sleep(sleep_time)
            self.last_refresh = time.time()

            for position, block in self.screen.items():
                x, y = position
                window.addch(y + 2, x + 2, self.BLOCK_ICONS[block])
            window.addstr(1, 1, f'Score: {self.score}')
            window.addstr(SCREEN_Y + 3, 1, 'Use the left and right arrows to move the paddle.')

    interpreter = IntcodeInterpreter()
    interpreter.evaluate(hacked_program, env=PlayArcadeGame())


if __name__ == '__main__':
    logging.basicConfig(filename='logging.out', level=logging.INFO)
    curses.initscr()
    try:
        curses.wrapper(play_game)
    except:
        logging.exception("Hurr durr")
    print()
