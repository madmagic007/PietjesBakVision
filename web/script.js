const ws = new WebSocket("ws://localhost:8080/ws/game");

ws.onmessage = e => {
    console.log("from server:", e.data);
    let o = JSON.parse(e.data);

    switch (o["action"]) {
        case "detectionState":
            let lbl = document.getElementById("detectionLabel")

            if (o["detectionState"]) {
                lbl.classList.add("detectionActive");
                lbl.classList.remove("detectionInActive");
            } else {
                lbl.classList.remove("detectionActive");
                lbl.classList.add("detectionInActive");
            }

            break;
    }
};

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

function setupTable(data) {
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

    data.names.forEach(name => {
        const row = document.createElement('tr');

        const nameCell = document.createElement('td');
        nameCell.textContent = name;
        nameCell.id = 'name_' + name;
        nameCell.classList.add('cellName');
        row.appendChild(nameCell);

        const pointsCell = document.createElement('td');
        pointsCell.textContent = '9';
        pointsCell.id = 'points_' + name;
        pointsCell.classList.add('cellPoints');
        row.appendChild(pointsCell);

        const highestCell = document.createElement('td');
        highestCell.textContent = '260';
        highestCell.id = 'highest_' + name;
        highestCell.classList.add('cellHighest');
        row.appendChild(highestCell);


        const statusCell = document.createElement('td');
        statusCell.textContent = '';
        statusCell.id = 'status_' + name;
        statusCell.classList.add('cellStatus');
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

const myData = {
    names: ['PlayerA', 'PlayerB', 'PlayerC', "PlayerD"]
};

setupTable(myData);