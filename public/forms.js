$(function() {
  function updateExplanations(name) {
    var sel = "";
    if (typeof name !== 'undefined') {
      sel = "="+name;
    }

    $("textarea[explains"+sel+"]").each(function() {
      var radio = $(this).siblings(".radio").children("input")

      if (radio.is(":checked")) {
        $(this).prop('disabled', false);
        $(this).removeClass('disabled');
        $(this).addClass('required');
      } else {
        $(this).prop('disabled', true);
        $(this).addClass('disabled');
        $(this).removeClass('required');
      }
    });
  }


  $("input[alternative]").change(function() {
    updateExplanations($(this).attr("name"))
  })

  updateExplanations()

  $(".help-tooltip").tooltip()

  $("a[data-confirm]").unbind('click').click(function(e) {
    jsConfirm($(this).attr("data-confirm"), $(this).attr("data-confirm-button"), $(this).attr("href"));
    e.preventDefault();
  });

  function jsConfirm(body, button, url) {
    $("body").append('<div class="modal" id="confirmModal" tabindex="-1" role="dialog" aria-labelledby="contactModalLabel" aria-hidden="true"> <div class="modal-dialog"> <div class="modal-content"><div class="modal-header"> <button typ    e="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button> <h4 class="modal-title" id="contactModalLabel">Confirmation</h4> </div> <div class="modal-body">'+body+'</div> <div class="modal-footer"> <button type=    "button" class="btn btn-default" data-dismiss="modal">Cancel</button> <button type="button" class="btn btn-danger" id="confirmModalSubmit">'+button+'</button> </div></div> </div> </div>')

    $("#confirmModal").modal()

    $("#confirmModalSubmit").click(function(e) {
      e.preventDefault()

      window.location.href = url
    })

    $("#confirmModal").on('hidden.bs.modal', function () {
      $(this).removeData('modal')
      $(this).remove()
    })
  }

  var usersSearch = new Bloodhound({
    remote: '/'+$("body").attr("data-faculty")+'/admin/lookup/?q=%QUERY',
    datumTokenizer: function(d) {
      return Bloodhound.tokenizers.whitespace(d.val);
    },
    queryTokenizer: Bloodhound.tokenizers.whitespace
  });

  usersSearch.initialize()

  $(".lookup").typeahead(null, {
     name: "users",
     valueKey: "sciper",
     displayKey: "sciper",
     source: usersSearch.ttAdapter(),
     templates: {
        empty: [
          '<div class="empty-message">',
          'Nothing found :(',
          '</div>'
        ].join('\n'),
        suggestion: Handlebars.compile('<p><strong>{{sciper}}</strong> – {{firstname}} {{lastname}}</p>')
    }
  });

  $(".open-format").click(function() {
    $("body").append('<div class="modal" id="formatModal" tabindex="-1" role="dialog" aria-labelledby="formatModalLabel" aria-hidden="true"> <div class="modal-dialog modal-lg"> <div class="modal-content"> </div> </div> </div>')
    $("#formatModal").modal({
      remote: '/'+$("body").attr("data-faculty")+'/admin/importformat',
      backdrop: "static"
    })
    
    $("#formatModal").on('hidden.bs.modal', function () {
      $(this).removeData('modal')
      $(this).remove()
    })
  });

  $("select.click-on-change").change(function() {
    var url = $('option:selected', this).attr('href')
    window.location.href = url
  });
});
