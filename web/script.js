let ws;
let reconnectTimer;

const hamburger = document.getElementById('hamburger');
const sideMenu = document.getElementById('sideMenu');

function wsConnect() {
    if (ws && (ws.readyState === WebSocket.OPEN || ws.readyState === WebSocket.CONNECTING)) {
        return;
    }

    ws = new WebSocket("ws://" + window.location.host + "/ws/game");

    ws.onmessage = e => {
        if (e.data == "pong") return;
        console.log("from server:", e.data);

        let o = JSON.parse(e.data);

        if ("detectionState" in o) {
            let lbl = document.getElementById("detectionLabel")
            let chk = document.getElementById("detection")

            if (o["detectionState"]) {
                lbl.classList.add("detectionActive");
                lbl.classList.remove("detectionInActive");
                chk.checked = true;
            } else {
                lbl.classList.remove("detectionActive");
                lbl.classList.add("detectionInActive");
                chk.checked = false;
            }
        }

        if ("throwVal" in o) {
            let throwVal = o["throwVal"];
            document.getElementById("fieldDetected").textContent = throwVal
        }

        let isSelfPlayer = false;
        let curPlayer = "";
        let winningPlayer = "";
        let maxThrows = -1;
        let highest = 0;

        if ("highestThisRound" in o) {
            highest = o["highestThisRound"]
            document.getElementById("fieldHighest").textContent = highest
        }

        if ("maxThrowsThisRound" in o) {
            maxThrows = o["maxThrowsThisRound"];
            document.getElementById("fieldThrowMax").textContent = maxThrows
        }

        if ("winningPlayer" in o) {
            winningPlayer = o["winningPlayer"];
        }

        if ("curPlayer" in o) {
            curPlayer = o["curPlayer"];
            let ownPlayerName = document.getElementById('inputPlayer').value;

            isSelfPlayer = curPlayer == ownPlayerName;

            let btns = document.getElementsByClassName("btnActionAny");
            Array.from(btns).forEach(btn => {
                btn.disabled = !isSelfPlayer;
            });
        } else if ("curThrowCount" in o) { // just need anything else
            let btns = document.getElementsByClassName("btnActionAny");
            Array.from(btns).forEach(btn => {
                btn.disabled = true;
            });
        }

        if ("curThrowCount" in o) {
            let cnt = o["curThrowCount"];
            document.getElementById("fieldThrowCnt").textContent = cnt;

            let stoef = document.getElementById("btnStoef");
            let newThrow = document.getElementById("btnNew");

            if (cnt > 1 || !isSelfPlayer || highest != 0) {
                stoef.disabled = true;
            } else if (isSelfPlayer && maxThrows > 1) {
                stoef.disabled = false;
            }

            if (cnt >= maxThrows) {
                newThrow.disabled = true;
            }
        }

        if ("players" in o) {
            setupScoreTable(o["players"], curPlayer, winningPlayer);
        }
    };

    ws.onopen = e => {
        ws.send("request");

        if (reconnectTimer) {
            clearTimeout(reconnectTimer);
            reconnectTimer = null;
        }
    }

    ws.onclose = () => {
        if (!reconnectTimer) {
            reconnectTimer = setTimeout(wsConnect, 1000);
        }
    };
}

wsConnect();

setInterval(() => {
    if (ws.readyState === WebSocket.OPEN) {
        ws.send('ping');
    }
}, 5000);

function sendWS(json) {
    const nameInput = document.getElementById('inputPlayer');
    json.playerName = nameInput.value;
    ws.send(JSON.stringify(json));

    hamburger.classList.remove('active');
    sideMenu.classList.remove('active');
}

function sendWSAction(action) {
    sendWS({action: action});
}

function sendWSAction(action, value) {
    let o = {
        action: action
    }

    o[action] = value;

    sendWS(o);
}

function promptSendWS(question, action) {
    let res = prompt(question);
    sendWSAction(action, res);
}

function setupScoreTable(players, curPlayer, winningPlayer) {
    const table = document.getElementById('tblScore');
    table.innerHTML = '';

    const headerRow = document.createElement('tr');

    const nameHeader = document.createElement('th');
    nameHeader.textContent = 'Naam';
    nameHeader.classList.add('cellName');
    headerRow.appendChild(nameHeader);

    const pointsHeader = document.createElement('th');
    pointsHeader.textContent = 'Punten';
    pointsHeader.classList.add('cellPoints');
    headerRow.appendChild(pointsHeader);

    const highestHeader = document.createElement('th');
    highestHeader.textContent = 'Hoogste deze ronde';
    highestHeader.classList.add('cellHighest');
    headerRow.appendChild(highestHeader);

    const statusHeader = document.createElement('th');
    statusHeader.textContent = '';
    statusHeader.classList.add('cellStatus');
    headerRow.appendChild(statusHeader);

    table.appendChild(headerRow);

    players.forEach(player => {
        const name = player["name"];
        const row = document.createElement('tr');

        if (name == winningPlayer) {
            row.classList.add('winning');
        } else {
            row.classList.remove('winning');
        }

        const nameCell = document.createElement('td');
        nameCell.textContent = name;
        nameCell.id = 'name_' + name;
        nameCell.classList.add('cellName');
        row.appendChild(nameCell);

        const pointsCell = document.createElement('td');
        pointsCell.textContent = player.points;
        pointsCell.id = 'points_' + name;
        pointsCell.classList.add('cellPoints');
        row.appendChild(pointsCell);

        const highestCell = document.createElement('td');
        highestCell.textContent = player.highestThisRound;
        highestCell.id = 'highest_' + name;
        highestCell.classList.add('cellHighest');
        row.appendChild(highestCell);


        const statusCell = document.createElement('td');
        statusCell.textContent = '';
        statusCell.id = 'status_' + name;
        statusCell.classList.add('cellStatus');

        if (player["hasStoeffed"]) {
            statusCell.classList.add("stoef");
        }

        if (curPlayer == name) {
            statusCell.classList.add("curPlayer");
        }

        if ("state" in player) {
            let state = player["state"];

            if (state == -1) statusCell.classList.add("dead");
            else if (state > 0){
                statusCell.style.setProperty('--status-content', `"\\f521     ${state}"`);
            }
        }

        row.appendChild(statusCell);

        table.appendChild(row);
    });
}

hamburger.addEventListener('click', () => {
    hamburger.classList.toggle('active');
    sideMenu.classList.toggle('active');
});