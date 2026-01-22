const ws = new WebSocket("ws://localhost:8080/ws/game");

ws.onopen = () => {
};

ws.onmessage = e => {
    console.log("from server:", e.data);
};

function sendWS(json) {
    const nameInput = document.getElementById('inputPlayer');
    json.playerName = nameInput.value;
    ws.send(JSON.stringify(json));
}

function sendWSAction(action) {
    sendWS({action: action});
}

function isNumberKey(evt) {
    var charCode = (evt.which) ? evt.which : evt.keyCode
    if (charCode > 31 && (charCode < 48 || charCode > 57))
    return false;
    return true;
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
        pointsCell.textContent = '69';
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

const myData = {
    names: ['PlayerA', 'PlayerB', 'PlayerC', "PlayerD"]
};

setupTable(myData);