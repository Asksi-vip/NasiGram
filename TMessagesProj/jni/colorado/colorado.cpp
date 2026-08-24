#include <string_view>
#include <dirent.h>
#include <unistd.h>
#include <zlib.h>

#include "colorado.h"
#include "logging.h"
#include "obfs-string.h"
#include "utils.h"

void kill_self() {
    kill(getpid(), SIGKILL);
}

bool check_signature() {
    return true;
}