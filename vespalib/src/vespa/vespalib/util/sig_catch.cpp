// Copyright 2017 Yahoo Holdings. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.

#include "sig_catch.h"
#include "signalhandler.h"

namespace vespalib {

SigCatch::SigCatch()
{
    SignalHandler::PIPE.ignore();
    SignalHandler::INT.hook();
    SignalHandler::TERM.hook();
    SignalHandler::CHLD.hook();
}

bool
SigCatch::receivedStopSignal()
{
    return (SignalHandler::INT.check() ||
            SignalHandler::TERM.check());
}

bool
SigCatch::receivedChildSignal()
{
    return SignalHandler::CHLD.check();
}

void
SigCatch::clearChildSignalFlag()
{
    SignalHandler::CHLD.clear();
}

} // namespace vespalib
