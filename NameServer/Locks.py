from threading import Lock

class RWLock:
    def __init__(self):
        self.lock = Lock()
        self.readers = 0
        self.guard_lock = Lock()

    def acquire_read(self):
        with self.guard_lock:
            self.readers += 1
            if self.readers == 1:
                self.lock.acquire()

    def release_read(self):
        with self.guard_lock:
            self.readers -= 1
            if self.readers == 0:
                self.lock.release()

    def acquire_write(self):
        self.lock.acquire()

    def release_write(self):
        self.lock.release()
