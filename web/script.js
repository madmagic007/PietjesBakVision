const ws = new WebSocket("ws://" + window.location.host + "/ws/game");

ws.onmessage = e => {
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

    if ("highestThisRound" in o) {
        let highest = o["highestThisRound"]
        document.getElementById("fieldHighest").textContent = highest
    }

    if ("maxThrowsThisRound" in o) {
        let cnt = o["maxThrowsThisRound"];
        document.getElementById("fieldThrowMax").textContent = cnt
    }

    let isSelfPlayer = false;
    let curPlayer = "";
    let winningPlayer = "";

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
    }

    if ("curThrowCount" in o) {
        let cnt = o["curThrowCount"];
        document.getElementById("fieldThrowCnt").textContent = cnt;

        let btn = document.getElementById("btnStoef");
        if (cnt > 1 || !isSelfPlayer) {
            btn.disabled = true;
        } else if (isSelfPlayer) {
            btn.disabled = false;
        }
    }

    if ("players" in o) {
        setupScoreTable(o["players"], curPlayer, winningPlayer);
    }
};

ws.onopen = e => {
    ws.send("request")
}

function sendWS(json) {
    const nameInput = document.getElementById('inputPlayer');
    json.playerName = nameInput.value;
    ws.send(JSON.stringify(json));
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

        row.appendChild(statusCell);

        table.appendChild(row);
    });
}

const hamburger = document.getElementById('hamburger');
const sideMenu = document.getElementById('sideMenu');

hamburger.addEventListener('click', () => {
    hamburger.classList.toggle('active');
    sideMenu.classList.toggle('active');
});