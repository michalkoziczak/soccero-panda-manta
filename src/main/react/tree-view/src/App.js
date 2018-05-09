import React, { Component } from 'react';
import Graph from 'vis-react';
import ReactDropdown from 'react-dropdown';
import 'react-dropdown/style.css'
import _ from 'lodash'
import './App.css';

var events = {
}

var websocket;

class App extends Component {

    constructor(props) {
        super(props);

        var options = this.generateGraphOptions();
        var size = this.detectSize();

        this.state = {
            selectedGraph: {nodes:[], edges:[]},
            options: options,
            competitions: [],
            graphs: {},
            limitedGraphs: {},
            selectedCompetition: window.location.hash.substring(1).replace(/_!_/gi, " "),
            x: size.x,
            y: size.y
        }

        setInterval(this.checkWebSocket.bind(this), 1000);
    }

   componentDidMount() {
       this.attachWebSocket();
       window.onresize = this.handleResize.bind(this);
       this.handleResize();
   }

   handleResize() {
       var selectedCompetition = this.state.selectedCompetition;
       this.setState({selectedCompetition: 'none'});
       this.setState({options: this.generateGraphOptions()});
       this.setState({selectedCompetition: selectedCompetition});
   }

   detectSize() {
     var w = window;
          var e = document.documentElement;
          var selector = document.getElementById('graph-selector');
          var g = document.getElementsByTagName('body')[0];

          var pageX = w.innerWidth || e.clientWidth || g.clientWidth;
          var pageY = w.innerHeight|| e.clientHeight || g.clientHeight;

          var selectorY = 0;

          if (selector !== null) {
              selectorY = selector.clientHeight || 0;
          }

          var x = pageX;
          var y = pageY - selectorY;

     return {x: x, y: y};
   }

   generateGraphOptions() {
     var size = this.detectSize();
     return {
          layout: {
              hierarchical: {
                direction: "LR",
                nodeSpacing: 40,
                treeSpacing: 90,
                levelSeparation: 350,
                sortMethod: 'directed',
                parentCentralization: true,
                blockShifting: true,
                edgeMinimization: true
              }
          },
          edges: {
              color: "#000000"
          },
          height: size.y + "px",
          width: size.x + "px",
          physics: {enabled: false},
          interaction: {
              dragNodes: false,
              hover: true,
              selectable: false
          }
     };
   }

   attachWebSocket() {
       if (websocket) {
         websocket.close();
       }
       websocket = new WebSocket("ws://soccero-panda-manta.playroom.leanforge.pl/tournaments");
       websocket.onmessage = this.handleTournamentChange.bind(this);
       websocket.onclose = this.attachWebSocket.bind(this);
       websocket.onopen = function() {
           websocket.send("init")
       };
   }

   checkWebSocket() {
       if (!websocket) {
           return;
       }

       if (websocket.readyState > 1) {
           this.attachWebSocket();
       }
   }

   calculateGraph(tree, minRound) {
     const layoutNodeId = tree.leagueName + '-' + tree.competition;
     const layoutNode = {
       hidden: false,
       id: layoutNodeId,
       level: 0,
       label: tree.competition,
       shape: 'text',
       chosen: false
     };
     const treeNodes = _.filter(tree.nodes, n => n.round >= minRound)
     const nodes = _.map(treeNodes, function(node) {
        let color = undefined;

        if (node.state === 'WON') {
         color = "#d3ffd1";
        }

        if (node.state === 'LOST') {
         color = "#ffc600";
        }

        if (node.state === 'ELIMINATED') {
         color = "#ff9090";
        }

        return {
            id: node.id,
            label: node.label,
            level: node.round + 1 - minRound,
            color: color,
            shape: 'box',
            fixed: true
        };
     });
     nodes.push(layoutNode);

     let edges = _.map(treeNodes, function(node) {
       if (!node.child) {
         return null;
       }
       return {from: node.id, to: node.child};
     });

     const layoutEdges = _.map(treeNodes, function(node) {
       const parent = _.find(treeNodes, parent => parent.child === node.id);
       if (parent) {
         return null;
       }
       return {from: layoutNodeId, to: node.id, hidden: true};
     });

     edges = edges.concat(layoutEdges);
     edges = _.filter(edges, function(edge) {return edge !== null});

     return {nodes: nodes, edges: edges};
   }

   handleTournamentChange(event) {
     const tree = JSON.parse(event.data);
     let graphs = this.state.graphs;
     let limitedGraphs = this.state.limitedGraphs;
     const graph = this.calculateGraph(tree, 0);
     const currentRound = _.maxBy(tree.nodes, n => n.round).round;
     let minRound = currentRound - 1;
     if (minRound < 0) {
       minRound = 0;
     }
     const limitedGraph = this.calculateGraph(tree, minRound);
     graphs['Summary'] = {nodes: [], edges: []};
     graphs[tree.leagueName + " " + tree.competition] = graph;
     limitedGraphs[tree.leagueName + " " + tree.competition] = limitedGraph;
     graphs['Summary'] = this.treeSummary(limitedGraphs);
     const competitions = _.keys(graphs);

     this.setState({
       graphs,
       competitions,
       limitedGraphs
     });

     if (this.state.selectedCompetition === tree.leagueName + " " + tree.competition) {
       this.setState({selectedGraph: graph})
     }
   }

   treeSummary(graphs) {
     if (_.values(graphs).length === 0) {
       return {nodes: [], edges: []};
     }
     var allGraphs = _.values(graphs);
     var nodes = [];
     var edges = [];
     _.forEach(allGraphs, function(v) {
       _.forEach(v.nodes, n => nodes.push(n));
       _.forEach(v.edges, e => edges.push(e));
     });
     return {
       nodes: nodes,
       edges: edges
     };
   }

   _onSelect(e) {
     var graphs = this.state.graphs || {};
     window.location.hash = e.value.replace(/ /gi, '_!_');
     var graph = graphs[e.value];
     if (!graph) {
       this.setState({selectedGraph: {nodes:[], edges:[]}});
       return;
     }

     this.setState({selectedGraph: graph, selectedCompetition: e.value});
   }

  render() {
    var selectedCompetition = this.state.selectedCompetition;
    var selectedGraph = this.state.selectedGraph;

    if (this.state.selectedCompetition === 'none') {
      return <div>
      <div id="graph-selector"><ReactDropdown value={selectedCompetition} options={this.state.competitions} placeholder="Select competition" onChange={this._onSelect.bind(this)}></ReactDropdown></div>
      <div id="graph-view"><Graph graph={{}} options={this.state.options} events={events}  /></div>
      </div>;
    }

    if (!this.state.selectedCompetition && this.state.competitions.length > 0) {
      selectedCompetition = this.state.competitions[0];
      selectedGraph = this.state.graphs[selectedCompetition];
    }

    if (this.state.selectedCompetition && this.state.graphs[selectedCompetition]) {
        selectedGraph = this.state.graphs[selectedCompetition];
    }

    return (
      <div>
      <div id="graph-selector"><ReactDropdown value={selectedCompetition} options={this.state.competitions} placeholder="Select competition" onChange={this._onSelect.bind(this)}></ReactDropdown></div>
      <div id="graph-view"><Graph graph={selectedGraph} options={this.state.options} events={events}  /></div>
      </div>

    );
  }
}

export default App;
